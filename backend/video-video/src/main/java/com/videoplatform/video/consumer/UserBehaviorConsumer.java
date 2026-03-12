package com.videoplatform.video.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.common.dto.UserBehaviorMsg;
import com.videoplatform.common.entity.VideoTag;
import com.videoplatform.common.entity.VideoTagRelation;
import com.videoplatform.video.mapper.VideoTagMapper;
import com.videoplatform.video.mapper.VideoTagRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户行为消息消费者 (Stage 5.1)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorConsumer {

    private final VideoTagRelationMapper videoTagRelationMapper;
    private final VideoTagMapper videoTagMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private static final String PROFILE_KEY_PREFIX = "user:profile:";
    private static final String SEEN_KEY_PREFIX = "user:seen:";

    @KafkaListener(topics = UserBehaviorMsg.TOPIC_BEHAVIOR, groupId = "video-recommendation-group")
    public void consumeBehavior(UserBehaviorMsg msg) {
        log.info("收到用户行为消息: user={}, video={}, type={}", msg.getUserId(), msg.getVideoId(), msg.getBehaviorType());

        try {
            // 1. 获取视频对应的所有标签ID
            List<VideoTagRelation> relations = videoTagRelationMapper.selectList(
                    new LambdaQueryWrapper<VideoTagRelation>()
                            .eq(VideoTagRelation::getVideoId, msg.getVideoId())
            );

            if (relations.isEmpty()) {
                log.warn("视频 {} 没有设置任何标签，无法更新推荐画像", msg.getVideoId());
                return;
            }

            List<Integer> tagIds = relations.stream()
                    .map(VideoTagRelation::getTagId)
                    .collect(Collectors.toList());

            // 2. 获取标签名称
            List<VideoTag> tags = videoTagMapper.selectBatchIds(tagIds);
            List<String> tagNames = tags.stream()
                    .map(VideoTag::getName)
                    .collect(Collectors.toList());

            log.info("视频 {} 的标签为: {}", msg.getVideoId(), tagNames);

            // 3. 将标签分值累加到 Redis 用户画像中 (Stage 5.2)
            String redisKey = PROFILE_KEY_PREFIX + msg.getUserId();
            Double score = msg.getScore() != null ? msg.getScore() : msg.getBehaviorType().getDefaultScore();
            
            for (String tagName : tagNames) {
                // 使用 HINCRBYFLOAT 累加标签分值
                redisTemplate.opsForHash().increment(redisKey, tagName, score);
            }
            
            // 设置 30 天滑动过期
            redisTemplate.expire(redisKey, java.time.Duration.ofDays(30));
            
            // 4. 精准曝光去重：只要有任何行为产生，就标记为已看 (6.1 & 6.2 优化)
            if (msg.getUserId() != null) {
                String seenKey = SEEN_KEY_PREFIX + msg.getUserId();
                redisTemplate.opsForSet().add(seenKey, String.valueOf(msg.getVideoId()));
                redisTemplate.expire(seenKey, java.time.Duration.ofDays(1)); // 设置24小时过期
            }
            
            log.info("用户画像更新成功: user={}, tags={}, added_score={}", msg.getUserId(), tagNames, score);
            
        } catch (Exception e) {
            log.error("处理用户行为消息失败", e);
        }
    }
}
