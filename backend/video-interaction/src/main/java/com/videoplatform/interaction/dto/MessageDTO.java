package com.videoplatform.interaction.dto;

import lombok.Data;

@Data
public class MessageDTO {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private Integer type;
    private String content;
    private Long relatedId;
    private Integer isRead;
    private String createdAt;
}
