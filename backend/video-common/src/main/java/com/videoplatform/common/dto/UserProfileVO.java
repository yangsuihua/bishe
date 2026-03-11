package com.videoplatform.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 用户资料VO
 */
@Data
public class UserProfileVO {
    
    private Long id;
    
    private String username;
    
    private String nickname;
    
    private String avatar;
    
    private String bio;
    
    private Integer gender;
    
    private Integer followerCount;
    
    private Integer followingCount;
    
    private Long likeCount;
    
    private Integer videoCount;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate birthday;
    
    /**
     * 是否已关注(当前用户视角)
     */
    private Boolean isFollowing;
    
    private Integer isFavoritesVisible;  // 收藏列表是否公开: 0-私密, 1-公开
    
    private Integer isLikesVisible;      // 点赞列表是否公开: 0-私密, 1-公开
}
