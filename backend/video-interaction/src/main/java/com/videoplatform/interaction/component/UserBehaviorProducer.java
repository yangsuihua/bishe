package com.videoplatform.interaction.component;

import com.videoplatform.common.dto.UserBehaviorMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 用户行为消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(Long userId, Long videoId, UserBehaviorMsg.BehaviorType type) {
        UserBehaviorMsg msg = UserBehaviorMsg.builder()
                .userId(userId)
                .videoId(videoId)
                .behaviorType(type)
                .score(type.getDefaultScore())
                .timestamp(System.currentTimeMillis())
                .build();
        
        try {
            kafkaTemplate.send(UserBehaviorMsg.TOPIC_BEHAVIOR, videoId.toString(), msg);
            log.info("用户行为消息发送成功: user={}, video={}, type={}", userId, videoId, type);
        } catch (Exception e) {
            log.error("用户行为消息发送失败", e);
        }
    }
}
