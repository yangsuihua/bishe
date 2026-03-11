package com.videoplatform.interaction.service;

import com.videoplatform.common.entity.UserLike;
import com.videoplatform.common.entity.Video;

import java.util.List;

/**
 * 点赞服务接口
 */
public interface LikeService {
    
    /**
     * 点赞视频
     */
    void likeVideo(Long userId, Long videoId);
    
    /**
     * 取消点赞
     */
    void unlikeVideo(Long userId, Long videoId);
    
    /**
     * 检查是否点赞
     */
    boolean isLiked(Long userId, Long videoId);
    
    List<Video> listLikedVideos(Long userId, Integer page, Integer size);
    
    List<UserLike> listLikedRecords(Long userId, Integer page, Integer size);
}