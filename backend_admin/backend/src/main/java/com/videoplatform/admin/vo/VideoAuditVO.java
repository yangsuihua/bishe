package com.videoplatform.admin.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class VideoAuditVO {
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Long userId;
    private String username;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    // 审核失败原因（最新的一条拒绝记录）
    private String lastRejectReason;
    // 最新审核记录的状态（1-通过, 0-拒绝, null-无审核记录）
    private Integer latestAuditStatus;
    // 是否有最新审核记录（用于判断是否显示操作按钮）
    private Boolean hasLatestAuditLog;
}
