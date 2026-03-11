package com.videoplatform.common.dto;

import lombok.Data;

/**
 * 用户更新DTO
 */
@Data
public class UserUpdateDTO {
    
    private String nickname;
    
    private String bio;
    
    private Integer gender;
    
    private String birthday;
    
    private Integer isFavoritesVisible;  // 收藏列表是否公开: 0-私密, 1-公开
    
    private Integer isLikesVisible;      // 点赞列表是否公开: 0-私密, 1-公开
    
    private String oldPassword;          // 旧密码
    
    private String newPassword;          // 新密码
    
    private String confirmNewPassword;   // 确认新密码
}
