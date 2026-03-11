package com.videoplatform.video.dto;

import lombok.Data;

/**
 * 视频发布DTO
 */
@Data
public class VideoPublishDTO {
    
    private String title;
    private String description;
    private String videoUrl;
    private String coverUrl;
    private Integer categoryId;
    private String tags;
    private Integer isPrivate;
}
