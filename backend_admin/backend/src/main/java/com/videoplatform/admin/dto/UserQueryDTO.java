package com.videoplatform.admin.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
    private Integer page = 1;
    private Integer size = 10;
    private Integer status; // 0-禁用, 1-正常
    private String role; // user/admin
    private String keyword; // 搜索关键词
}
