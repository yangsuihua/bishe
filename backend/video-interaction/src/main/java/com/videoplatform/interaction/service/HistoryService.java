package com.videoplatform.interaction.service;

import com.videoplatform.common.entity.UserHistory;

import java.util.List;

public interface HistoryService {
    void recordHistory(Long userId, Long videoId, Integer watchDuration, Double watchProgress);

    List<UserHistory> listHistory(Long userId, Integer page, Integer size);

    void deleteHistory(Long userId, Long historyId);
}
