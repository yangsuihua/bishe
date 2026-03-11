package com.videoplatform.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private Long videoId;
    private Integer auditStatus; // 1-通过, 0-不通过
    private String reason;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
