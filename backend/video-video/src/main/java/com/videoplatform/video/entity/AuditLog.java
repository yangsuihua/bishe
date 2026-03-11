package com.videoplatform.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核日志实体（用于查询sys_log表）
 */
@Data
@TableName("sys_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long videoId;
    private Integer auditStatus; // 1-通过, 0-不通过
    private String reason;
    private LocalDateTime createTime;
}
