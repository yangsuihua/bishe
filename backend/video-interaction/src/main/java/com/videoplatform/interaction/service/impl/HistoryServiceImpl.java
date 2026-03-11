package com.videoplatform.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.common.entity.UserHistory;
import com.videoplatform.interaction.mapper.UserHistoryMapper;
import com.videoplatform.interaction.mapper.VideoMapper;
import com.videoplatform.interaction.service.HistoryService;
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
public class HistoryServiceImpl implements HistoryService {

    private final UserHistoryMapper userHistoryMapper;
    private final VideoMapper videoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordHistory(Long userId, Long videoId, Integer watchDuration, Double watchProgress) {
        if (userId == null || videoId == null) {
            return;
        }
        UserHistory exist = userHistoryMapper.selectOne(new LambdaQueryWrapper<UserHistory>()
                .eq(UserHistory::getUserId, userId)
                .eq(UserHistory::getVideoId, videoId));
        LocalDateTime now = LocalDateTime.now();
        
        // 判断是否是新的视频浏览（watchDuration=0且watchProgress=0表示新浏览开始）
        boolean isNewView = (watchDuration != null && watchDuration == 0 && 
                            watchProgress != null && watchProgress == 0);
        
        if (exist == null) {
            // 第一次观看该视频，创建新记录
            UserHistory history = new UserHistory();
            history.setUserId(userId);
            history.setVideoId(videoId);
            history.setWatchDuration(watchDuration);
            history.setWatchProgress(watchProgress);
            history.setCreatedAt(now);
            history.setUpdatedAt(now);
            userHistoryMapper.insert(history);
            
            // 如果是新浏览（watchDuration=0且watchProgress=0），增加视频播放量
            // 这样确保一次视频浏览只增加一次，且不在3-5秒更新的逻辑里增加
            if (isNewView) {
                LambdaUpdateWrapper<com.videoplatform.common.entity.Video> videoUpdate = 
                    new LambdaUpdateWrapper<com.videoplatform.common.entity.Video>()
                        .eq(com.videoplatform.common.entity.Video::getId, videoId)
                        .setSql("view_count = view_count + 1");
                videoMapper.update(null, videoUpdate);
                log.info("视频播放量+1, videoId: {}, userId: {}", videoId, userId);
            }
        } else {
            // 之前已经观看过该视频，更新记录
            LambdaUpdateWrapper<UserHistory> update = new LambdaUpdateWrapper<UserHistory>()
                    .eq(UserHistory::getId, exist.getId())
                    .set(UserHistory::getWatchDuration, watchDuration)
                    .set(UserHistory::getWatchProgress, watchProgress)
                    .set(UserHistory::getUpdatedAt, now);
            userHistoryMapper.update(null, update);
            
            // 如果是新浏览（watchDuration=0且watchProgress=0），说明用户重新观看该视频，增加播放量
            // 这样确保一个用户可以增加多次（每次新浏览都增加），但一次浏览只增加一次
            if (isNewView) {
                LambdaUpdateWrapper<com.videoplatform.common.entity.Video> videoUpdate = 
                    new LambdaUpdateWrapper<com.videoplatform.common.entity.Video>()
                        .eq(com.videoplatform.common.entity.Video::getId, videoId)
                        .setSql("view_count = view_count + 1");
                videoMapper.update(null, videoUpdate);
                log.info("视频播放量+1（用户重新观看）, videoId: {}, userId: {}", videoId, userId);
            }
        }
    }

    @Override
    public List<UserHistory> listHistory(Long userId, Integer page, Integer size) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        Page<UserHistory> pager = new Page<>(current, pageSize);
        LambdaQueryWrapper<UserHistory> wrapper = new LambdaQueryWrapper<UserHistory>()
                .eq(UserHistory::getUserId, userId)
                .orderByDesc(UserHistory::getUpdatedAt);
        userHistoryMapper.selectPage(pager, wrapper);
        return pager.getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHistory(Long userId, Long historyId) {
        if (userId == null || historyId == null) {
            return;
        }
        userHistoryMapper.delete(new LambdaQueryWrapper<UserHistory>()
                .eq(UserHistory::getId, historyId)
                .eq(UserHistory::getUserId, userId));
    }
}
