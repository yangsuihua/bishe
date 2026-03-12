package com.videoplatform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户行为消息 DTO (推荐系统埋点使用)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorMsg implements Serializable {
    
    private Long userId;
    private Long videoId;
    private BehaviorType behaviorType;
    private Double score;
    private Long timestamp;

    /**
     * 行为类型枚举
     */
    public enum BehaviorType {
        CLICK(1.0),    // 点击播放
        LIKE(2.0),     // 点赞
        COLLECT(3.0),  // 收藏
        COMMENT(4.0),  // 评论
        SHARE(5.0),    // 分享
        WATCH_80(5.0), // 完播 (观看超过80%)
        UNLIKE(-2.0);  // 取消点赞

        private final Double defaultScore;

        BehaviorType(Double defaultScore) {
            this.defaultScore = defaultScore;
        }

        public Double getDefaultScore() {
            return defaultScore;
        }
    }
}
