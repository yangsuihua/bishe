package com.videoplatform.search.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoDoc {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private List<String> tags;
    private Long viewCount;
    private String createdAt; // 对应 ResultDTO 的 createdAt
    private String coverUrl;
    private String username;
    private String avatar;
    
    // ES Completion Suggester 字段
    private Suggestion suggestion;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Suggestion {
        private List<String> input;
        private Long weight;
    }
}
