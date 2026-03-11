package com.videoplatform.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_tag_relation")
public class VideoTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long videoId;
    private Integer tagId;
    private LocalDateTime createdAt;
}
