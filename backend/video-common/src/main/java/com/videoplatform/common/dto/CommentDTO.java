package com.videoplatform.common.dto;

import lombok.Data;

/**
 * 评论DTO
 */
@Data
public class CommentDTO {
    
    private Long id;
    private Long videoId;
    private Long userId;
    private String username;
    private String avatar;
    private Long parentId;
    private Long replyUserId;
    private String replyUsername;
    private String content;

    private String createdAt;
    private Boolean isLiked;
}
