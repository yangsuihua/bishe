package com.videoplatform.admin.dto;

import lombok.Data;

@Data
public class VideoAuditDTO {
    private String action; // approve/reject
    private String reason; // 拒绝原因
}
