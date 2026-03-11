package com.videoplatform.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系实体类
 * 对应数据库表: friendship
 */
@Data
@TableName("friendship")
public class Friendship {

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID较小者
     * 对应字段: user_id_1
     */
    @TableField("user_id_1")
    private Long userId1;
    
    /**
     * 用户ID较大者
     * 对应字段: user_id_2
     */
    @TableField("user_id_2")
    private Long userId2;
    
    /**
     * 成为好友时间
     * 对应字段: create_time
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}