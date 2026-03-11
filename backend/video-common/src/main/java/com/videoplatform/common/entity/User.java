package com.videoplatform.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String password;
    private String avatar;
    private String bio;
    private Integer gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate birthday;
    private Integer status;
    private String role;
    private Integer followerCount;
    private Integer followingCount;
    private Long likeCount;
    private Integer videoCount;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer isFavoritesVisible;  // 收藏列表是否公开: 0-私密, 1-公开
    private Integer isLikesVisible;      // 点赞列表是否公开: 0-私密, 1-公开

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    public Integer getFollowingCount() {
        return followingCount;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }
}

