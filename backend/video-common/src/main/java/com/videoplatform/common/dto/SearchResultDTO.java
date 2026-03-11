package com.videoplatform.common.dto;

import lombok.Data;

@Data
public class SearchResultDTO {
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private Long userId;
    private String username;
    private String avatar;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private String createdAt;
    private String type; // "video" 或 "user"
}
