package com.videoplatform.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_tag")
public class VideoTag {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer videoCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
