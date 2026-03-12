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
    
    public static final String TOPIC_BEHAVIOR = "user-behavior-topic";
    
    private Long userId;
    private Long videoId;
    private BehaviorType behaviorType;
    private Double score;
    private Long timestamp;

    /**
     * 行为类型枚举 (分值参考实施计划 3.1)
     */
    public enum BehaviorType {
        LIKE(2.0),     // 点赞 (+2)
        COMMENT(3.0),  // 评论 (+3)
        COLLECT(4.0),  // 收藏 (+4)
        WATCH_80(1.0), // 完播 (观看超过80%, +1)
        WATCH_START(0.0), // 起播 (用于标记已看/去重, 0分)
        UNLIKE(-2.0),  // 取消点赞
        UNCOLLECT(-4.0); // 取消收藏

        private final Double defaultScore;

        BehaviorType(Double defaultScore) {
            this.defaultScore = defaultScore;
        }

        public Double getDefaultScore() {
            return defaultScore;
        }
    }
}
