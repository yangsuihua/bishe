package com.videoplatform.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.dto.UserQueryDTO;
import com.videoplatform.admin.entity.User;
import com.videoplatform.admin.mapper.UserMapper;
import com.videoplatform.admin.mapper.VideoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManageService {
    
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    
    /**
     * 分页查询用户列表
     */
    public Page<User> getUserList(UserQueryDTO dto) {
        Page<User> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(User::getDeleted, 0);
        
        if (dto.getStatus() != null) {
            wrapper.eq(User::getStatus, dto.getStatus());
        }
        if (StringUtils.hasText(dto.getRole())) {
            wrapper.eq(User::getRole, dto.getRole());
        }
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, dto.getKeyword())
                    .or().like(User::getNickname, dto.getKeyword())
                    .or().like(User::getEmail, dto.getKeyword()));
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        
        return userMapper.selectPage(page, wrapper);
    }
    
    /**
     * 获取用户详情
     */
    public User getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
    
    /**
     * 更新用户信息
     */
    @Transactional
    public void updateUser(Long id, User user) {
        User existUser = userMapper.selectById(id);
        if (existUser == null || existUser.getDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        if (StringUtils.hasText(user.getNickname())) {
            existUser.setNickname(user.getNickname());
        }
        if (StringUtils.hasText(user.getEmail())) {
            existUser.setEmail(user.getEmail());
        }
        if (StringUtils.hasText(user.getPhone())) {
            existUser.setPhone(user.getPhone());
        }
        if (StringUtils.hasText(user.getBio())) {
            existUser.setBio(user.getBio());
        }
        if (user.getGender() != null) {
            existUser.setGender(user.getGender());
        }
        
        userMapper.updateById(existUser);
    }
    
    /**
     * 启用/禁用用户
     */
    @Transactional
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setStatus(status);
        userMapper.updateById(user);
    }
    
    /**
     * 修改用户角色
     */
    @Transactional
    public void updateUserRole(Long id, String role) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setRole(role);
        userMapper.updateById(user);
    }
    
    /**
     * 获取用户的视频列表
     */
    public Page<com.videoplatform.admin.entity.Video> getUserVideos(Long userId, Integer page, Integer size) {
        Page<com.videoplatform.admin.entity.Video> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<com.videoplatform.admin.entity.Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.videoplatform.admin.entity.Video::getUserId, userId)
                .eq(com.videoplatform.admin.entity.Video::getDeleted, 0)
                .orderByDesc(com.videoplatform.admin.entity.Video::getCreatedAt);
        
        return videoMapper.selectPage(pageObj, wrapper);
    }
}
