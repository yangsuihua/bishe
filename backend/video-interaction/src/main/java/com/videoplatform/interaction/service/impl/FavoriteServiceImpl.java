package com.videoplatform.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.common.entity.UserFavorite;
import com.videoplatform.common.entity.Video;
import com.videoplatform.interaction.mapper.UserFavoriteMapper;
import com.videoplatform.interaction.mapper.VideoMapper;
import com.videoplatform.interaction.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;
    private final VideoMapper videoMapper;
    private final com.videoplatform.interaction.component.UserBehaviorProducer userBehaviorProducer;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return;
        }
        UserFavorite exist = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getVideoId, videoId));
        if (exist != null) {
            return;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setVideoId(videoId);
        favorite.setCreatedAt(LocalDateTime.now());
        userFavoriteMapper.insert(favorite);

        LambdaUpdateWrapper<Video> update = new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, videoId)
                .setSql("favorite_count = favorite_count + 1");
        videoMapper.update(null, update);

        // 发送行为消息到Kafka
        userBehaviorProducer.sendMessage(userId, videoId, com.videoplatform.common.dto.UserBehaviorMsg.BehaviorType.COLLECT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return;
        }
        int removed = userFavoriteMapper.delete(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getVideoId, videoId));
        if (removed > 0) {
            LambdaUpdateWrapper<Video> update = new LambdaUpdateWrapper<Video>()
                    .eq(Video::getId, videoId)
                    .setSql("favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END");
            videoMapper.update(null, update);

            // 发送行为消息到Kafka
            userBehaviorProducer.sendMessage(userId, videoId, com.videoplatform.common.dto.UserBehaviorMsg.BehaviorType.UNCOLLECT);
        }
    }

    @Override
    public boolean isFavorited(Long userId, Long videoId) {
        if (userId == null || videoId == null) {
            return false;
        }
        Long count = userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getVideoId, videoId));
        return count != null && count > 0;
    }

    @Override
    public List<Video> listFavorites(Long userId, Integer page, Integer size) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<UserFavorite> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreatedAt);
        userFavoriteMapper.selectPage(pager, wrapper);
        List<Long> videoIds = pager.getRecords().stream()
                .map(UserFavorite::getVideoId)
                .collect(java.util.stream.Collectors.toList());
        if (videoIds.isEmpty()) {
            return Collections.emptyList();
        }
        return videoMapper.selectBatchIds(videoIds);
    }
    
    @Override
    public List<UserFavorite> listFavoriteRecords(Long userId, Integer page, Integer size) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<UserFavorite> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreatedAt);
        return userFavoriteMapper.selectPage(pager, wrapper).getRecords();
    }
}
