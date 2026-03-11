package com.videoplatform.user.service;

import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.dto.UserUpdateDTO;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 获取用户资料
     */
    UserProfileVO getUserProfile(Long userId);
    
    /**
     * 更新用户信息
     */
    void updateUser(Long userId, UserUpdateDTO dto);
    
    /**
     * 更新头像（通过URL）
     */
    void updateAvatar(Long userId, String avatarUrl);
    
    /**
     * 更新头像（通过文件上传）
     */
    String updateAvatar(Long userId, org.springframework.web.multipart.MultipartFile avatar);
    
    /**
     * 批量获取用户信息
     */
    java.util.List<UserProfileVO> getUsersByIds(java.util.List<Long> userIds);
    
    /**
     * 更新用户获赞数
     */
    void updateLikeCount(Long userId, Integer delta);
}
