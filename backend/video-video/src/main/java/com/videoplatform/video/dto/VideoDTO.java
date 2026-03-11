package com.videoplatform.video.dto;

import lombok.Data;

/**
 * 视频DTO
 */
@Data
public class VideoDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer duration;
    private String category;
    private Long viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer favoriteCount;
    private String createdAt;
    
    // 当前用户相关
    private Boolean isLiked;
    private Boolean isFavorited;
    private Boolean isFollowing;
}
