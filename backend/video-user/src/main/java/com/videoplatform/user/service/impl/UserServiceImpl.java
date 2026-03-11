package com.videoplatform.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.videoplatform.common.exception.BusinessException;
import com.videoplatform.common.result.ResultCode;
import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.dto.UserUpdateDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.videoplatform.common.entity.User;
import com.videoplatform.user.mapper.UserMapper;
import com.videoplatform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final MinioClient minioClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Override
    public UserProfileVO getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);
        
        // 手动处理birthday字段转换
        if (user.getBirthday() != null) {
            vo.setBirthday(user.getBirthday());
        }
        
        return vo;
    }
    
    @Override
    public void updateUser(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        User updateUser = new User();
        updateUser.setId(userId);
        
        // 检查是否需要更新密码
        if (dto.getOldPassword() != null && dto.getNewPassword() != null && dto.getConfirmNewPassword() != null) {
            // 验证新密码和确认密码是否一致
            if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "新密码与确认密码不一致");
            }
            
            // 验证旧密码（使用BCrypt进行密码验证）
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "旧密码错误");
            }
            
            // 设置新密码（使用BCrypt进行加密）
            updateUser.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
        
        // 复制除密码相关字段外的其他属性
        updateUser.setNickname(dto.getNickname());
        updateUser.setBio(dto.getBio());
        updateUser.setGender(dto.getGender());
        
        // 处理生日字段转换
        if (dto.getBirthday() != null && !dto.getBirthday().isEmpty()) {
            try {
                java.time.LocalDate birthday = java.time.LocalDate.parse(dto.getBirthday());
                updateUser.setBirthday(birthday);
            } catch (Exception e) {
                // 如果日期格式不正确，可以记录日志或抛出异常
                System.out.println("生日日期格式错误: " + dto.getBirthday());
            }
        }
        
        updateUser.setIsFavoritesVisible(dto.getIsFavoritesVisible());
        updateUser.setIsLikesVisible(dto.getIsLikesVisible());
        
        userMapper.updateById(updateUser);
    }
    
    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);
    }
    
    @Override
    public String updateAvatar(Long userId, MultipartFile avatar) {
        try {
            // 1. 验证文件
            if (avatar.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR);
            }
            
            // 获取当前用户信息，用于删除旧头像
            User currentUser = userMapper.selectById(userId);
            if (currentUser != null && currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
                String oldAvatarUrl = currentUser.getAvatar();
                // 从URL中提取对象名
                String oldObjectName = extractObjectNameFromUrl(oldAvatarUrl);
                if (oldObjectName != null && !oldObjectName.isEmpty()) {
                    // 尝试删除旧头像文件
                    try {
                        minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(oldObjectName)
                                .build()
                        );
                    } catch (Exception e) {
                        // 删除旧文件失败不影响上传流程，记录日志即可
                        System.out.println("删除旧头像失败: " + e.getMessage());
                    }
                }
            }
            
            // 2. 生成唯一文件名 (防止重名覆盖)
            String originalFilename = avatar.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : ".jpg";
            String fileName = "avatar/" + userId + "/" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            // 3. 上传到MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(avatar.getInputStream(), avatar.getSize(), -1)
                    .contentType(avatar.getContentType())
                    .build()
            );
            
            // 4. 拼接访问URL
            String avatarUrl = minioEndpoint + "/" + bucketName + "/" + fileName;
            
            // 6. 更新数据库
            User user = new User();
            user.setId(userId);
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);
            
            // 7. 返回URL给前端回显
            return avatarUrl;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ResultCode.UPLOAD_FILE_ERROR, "头像上传失败: " + e.getMessage());
        }
    }
    
    // 从URL中提取对象名
    private String extractObjectNameFromUrl(String url) {
        if (url == null) return null;
        
        try {
            // 从URL中提取对象名部分 (例如从 http://localhost:9000/bucket/avatar/123.jpg 提取 avatar/123.jpg)
            int bucketIndex = url.indexOf("/" + bucketName + "/");
            if (bucketIndex != -1) {
                return url.substring(bucketIndex + bucketName.length() + 2);
            }
        } catch (Exception e) {
            System.out.println("解析头像URL失败: " + e.getMessage());
        }
        return null;
    }
    
    @Override
    public List<UserProfileVO> getUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .map(user -> {
                    UserProfileVO vo = new UserProfileVO();
                    BeanUtils.copyProperties(user, vo);
                    if (user.getBirthday() != null) {
                        vo.setBirthday(user.getBirthday());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateLikeCount(Long userId, Integer delta) {
        if (userId == null || delta == null) {
            return;
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .setSql("like_count = CASE WHEN like_count + " + delta + " >= 0 THEN like_count + " + delta + " ELSE 0 END")
                .eq(User::getId, userId));
    }

}

