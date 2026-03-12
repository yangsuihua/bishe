package com.videoplatform.video.service;

import com.videoplatform.common.entity.VideoCategory;
import com.videoplatform.common.entity.VideoTag;
import com.videoplatform.common.dto.VideoDTO;
import com.videoplatform.common.dto.VideoPublishDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频服务接口
 */
public interface VideoService {
    
    /**
     * 获取视频推荐流
     */
    List<VideoDTO> getVideoFeed(Integer page, Integer size, Long userId);
    
    /**
     * 获取视频详情
     */
    VideoDTO getVideoDetail(Long videoId);
    
    /**
     * 上传视频文件
     */
    String uploadVideoFile(MultipartFile file, Long userId);
    
    /**
     * 上传封面文件
     */
    String uploadCoverFile(MultipartFile file, Long userId);
    
    /**
     * 发布视频
     */
    Long publishVideo(Long userId, VideoPublishDTO dto);
    
    /**
     * 获取用户视频列表
     * @param userId 目标用户ID
     * @param page 页码
     * @param size 每页数量
     * @param currentUserId 当前登录用户ID（可选，如果提供且等于userId，则返回所有状态的视频；否则只返回已发布的视频）
     */
    List<VideoDTO> getUserVideos(Long userId, Integer page, Integer size, Long currentUserId);
    
    /**
     * 删除视频
     */
    void deleteVideo(Long userId, Long videoId);
    
    /**
     * 获取视频分类
     */
    List<VideoCategory> getCategories();
    
    /**
     * 根据分类ID获取视频列表
     */
    List<VideoDTO> getVideosByCategory(Integer categoryId, Integer page, Integer size);
    
    /**
     * 获取所有视频标签
     */
    List<VideoTag> getVideoTags();
    
    /**
     * 批量获取视频详情
     */
    List<VideoDTO> getVideosByIds(List<Long> videoIds);
    
    /**
     * 审核视频
     * @param videoId 视频ID
     * @param status 状态（1:通过, 2:驳回）
     * @param reason 审核意见
     */
    void auditVideo(Long videoId, Integer status, String reason);

    /**
     * 获取视频的最新审核失败原因
     */
    String getLatestRejectReason(Long videoId);
}