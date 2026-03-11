package com.videoplatform.common.dto;

import lombok.Data;

@Data
public class AdminStatsDTO {
    private Long userCount;
    private Long videoCount;
    private Long commentCount;
    private Long likeCount;
}
