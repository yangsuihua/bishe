package com.videoplatform.interaction.service;

import com.videoplatform.common.entity.UserFavorite;
import com.videoplatform.common.entity.Video;

import java.util.List;

public interface FavoriteService {
    void addFavorite(Long userId, Long videoId);

    void removeFavorite(Long userId, Long videoId);

    boolean isFavorited(Long userId, Long videoId);

    List<Video> listFavorites(Long userId, Integer page, Integer size);
    
    List<UserFavorite> listFavoriteRecords(Long userId, Integer page, Integer size);
}
