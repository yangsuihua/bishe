package com.videoplatform.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_tag")
public class VideoTag {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer videoCount;
    private LocalDateTime createdAt;
}

