package com.videoplatform.search.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotSearchResponse {
    private String type; // "today" 或 "recent"
    private Integer days; // 近N天（仅当type为recent时有效）
    private List<HotSearchItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotSearchItem {
        private String keyword;
        private Integer count;
    }
}
