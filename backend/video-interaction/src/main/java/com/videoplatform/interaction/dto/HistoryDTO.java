package com.videoplatform.interaction.dto;

import lombok.Data;

@Data
public class HistoryDTO {
    private Long videoId;
    private Integer watchDuration;
    private Double watchProgress;
}
