package com.videoplatform.admin.dto;

import lombok.Data;

@Data
public class VideoQueryDTO {
    private Integer page = 1;
    private Integer size = 10;
    private Integer status; // 0-待审核, 1-已审核, 2-审核失败
    private Integer categoryId;
    private Long userId;
    private String keyword; // 搜索关键词
}
