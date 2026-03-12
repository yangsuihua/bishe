package com.videoplatform.common.dto;

import lombok.Data;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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
    private java.util.List<String> tags;
}
