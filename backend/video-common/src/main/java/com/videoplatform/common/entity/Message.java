package com.videoplatform.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("from_user_id")
    private Long fromUserId;
    
    @TableField("to_user_id")
    private Long toUserId;
    
    /**
     * 消息类型: 0-纯文本, 1-视频分享
     */
    private Integer type;
    
    /**
     * 内容: type=0时为聊天文本; type=1时可存视频标题/封面JSON快照
     */
    private String content;
    
    /**
     * 关联ID: type=1时存储video_id
     */
    @TableField("related_id")
    private Long relatedId;
    
    /**
     * 是否已读: 0-未读, 1-已读
     */
    @TableField("is_read")
    private Integer isRead;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
