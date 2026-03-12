package com.videoplatform.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.common.entity.UserLike;
import com.videoplatform.common.entity.Video;
import com.videoplatform.common.feign.UserFeignClient;
import com.videoplatform.common.result.Result;
import com.videoplatform.interaction.mapper.UserLikeMapper;
import com.videoplatform.interaction.mapper.VideoMapper;
import com.videoplatform.interaction.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 点赞服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final UserLikeMapper userLikeMapper;
    private final VideoMapper videoMapper;
    private final UserFeignClient userFeignClient;
    private final com.videoplatform.interaction.component.UserBehaviorProducer userBehaviorProducer;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeVideo(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return;
        }
        UserLike existing = userLikeMapper.selectOne(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getVideoId, videoId));
        if (existing != null) {
            return;
        }
        
        // 获取视频信息以获取发布者ID
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return; // 视频不存在
        }
        Long videoOwnerId = video.getUserId();
        
        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setVideoId(videoId);
        like.setCreatedAt(LocalDateTime.now());
        userLikeMapper.insert(like);

        // 更新视频点赞数
        LambdaUpdateWrapper<Video> videoUpdate = new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, videoId)
                .setSql("like_count = like_count + 1");
        videoMapper.update(null, videoUpdate);
        
        // 更新视频发布者的获赞数（通过 Feign 调用）
        try {
            userFeignClient.updateLikeCount(videoOwnerId, 1);
        } catch (Exception e) {
            log.error("更新用户获赞数失败", e);
        }

        // 发送行为消息到Kafka
        userBehaviorProducer.sendMessage(userId, videoId, com.videoplatform.common.dto.UserBehaviorMsg.BehaviorType.LIKE);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeVideo(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return;
        }
        
        int removed = userLikeMapper.delete(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getVideoId, videoId));
        if (removed > 0) {
            // 获取视频信息以获取发布者ID
            Video video = videoMapper.selectById(videoId);
            if (video != null) {
                Long videoOwnerId = video.getUserId();
                
                // 更新视频点赞数
                LambdaUpdateWrapper<Video> videoUpdate = new LambdaUpdateWrapper<Video>()
                        .eq(Video::getId, videoId)
                        .setSql("like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END");
                videoMapper.update(null, videoUpdate);
                
                // 更新视频发布者的获赞数（通过 Feign 调用）
                try {
                    userFeignClient.updateLikeCount(videoOwnerId, -1);
                } catch (Exception e) {
                    log.error("更新用户获赞数失败", e);
                }

                // 发送行为消息到Kafka
                userBehaviorProducer.sendMessage(userId, videoId, com.videoplatform.common.dto.UserBehaviorMsg.BehaviorType.UNLIKE);
            }
        }
    }
    
    @Override
    public boolean isLiked(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return false;
        }
        Long count = userLikeMapper.selectCount(new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getVideoId, videoId));
        return count != null && count > 0;
    }
    
    @Override
    public List<Video> listLikedVideos(Long userId, Integer page, Integer size) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<UserLike> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<UserLike> wrapper = new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .orderByDesc(UserLike::getCreatedAt);
        userLikeMapper.selectPage(pager, wrapper);
        List<Long> videoIds = pager.getRecords().stream()
                .map(UserLike::getVideoId)
                .collect(java.util.stream.Collectors.toList());
        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }
        return videoMapper.selectBatchIds(videoIds);
    }
    
    @Override
    public List<UserLike> listLikedRecords(Long userId, Integer page, Integer size) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<UserLike> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<UserLike> wrapper = new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .orderByDesc(UserLike::getCreatedAt);
        return userLikeMapper.selectPage(pager, wrapper).getRecords();
    }
}
