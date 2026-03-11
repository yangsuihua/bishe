package com.videoplatform.admin.vo;

import lombok.Data;

@Data
public class AdminStatsVO {
    private Long totalUsers;
    private Long totalVideos;
    private Long pendingAuditVideos;
    private Long todayNewUsers;
}
