package com.videoplatform.video.controller;

import com.videoplatform.common.entity.VideoCategory;
import com.videoplatform.common.entity.VideoTag;
import com.videoplatform.common.result.Result;
import com.videoplatform.common.dto.VideoDTO;
import com.videoplatform.common.dto.VideoPublishDTO;
import com.videoplatform.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 视频控制器
 */
@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
    
    private final VideoService videoService;
    
    /**
     * 获取视频推荐流
     */
    @GetMapping("/feed")
    public Result<List<VideoDTO>> getVideoFeed(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<VideoDTO> videos = videoService.getVideoFeed(page, size, userId);
        return Result.success(videos);
    }
    
    /**
     * 获取视频详情
     */
    @GetMapping("/{videoId}")
    public Result<VideoDTO> getVideoDetail(@PathVariable Long videoId) {
        VideoDTO video = videoService.getVideoDetail(videoId);
        return Result.success(video);
    }
    
    /**
     * 上传视频文件
     */
    @PostMapping("/upload")
    public Result<String> uploadVideo(@RequestHeader("X-User-Id") Long userId, @RequestParam("file") MultipartFile file) {
        String videoUrl = videoService.uploadVideoFile(file, userId);
        return Result.success(videoUrl);
    }
    
    /**
     * 上传封面
     */
    @PostMapping("/upload/cover")
    public Result<String> uploadCover(@RequestHeader("X-User-Id") Long userId, @RequestParam("file") MultipartFile file) {
        String coverUrl = videoService.uploadCoverFile(file, userId);
        return Result.success(coverUrl);
    }
    
    /**
     * 发布视频
     */
    @PostMapping("/publish")
    public Result<Long> publishVideo(@RequestHeader("X-User-Id") Long userId,
                                      @RequestBody VideoPublishDTO dto) {
        Long videoId = videoService.publishVideo(userId, dto);
        return Result.success(videoId);
    }
    
    /**
     * 获取用户视频列表
     * @param userId 目标用户ID
     * @param currentUserId 当前登录用户ID（可选，如果提供且等于userId，则返回所有状态的视频；否则只返回已发布的视频）
     */
    @GetMapping("/user/{userId}")
    public Result<List<VideoDTO>> getUserVideos(@PathVariable Long userId,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        List<VideoDTO> videos = videoService.getUserVideos(userId, page, size, currentUserId);
        return Result.success(videos);
    }
    
    /**
     * 删除视频
     */
    @DeleteMapping("/{videoId}")
    public Result<Void> deleteVideo(@RequestHeader("X-User-Id") Long userId,
                                     @PathVariable Long videoId) {
        videoService.deleteVideo(userId, videoId);
        return Result.success();
    }
    
    /**
     * 获取视频分类
     */
    @GetMapping("/categories")
    public Result<List<VideoCategory>> getCategories() {
        return Result.success(videoService.getCategories());
    }
    
    /**
     * 根据分类获取视频
     */
    @GetMapping("/category/{categoryId}")
    public Result<List<VideoDTO>> getVideosByCategory(@PathVariable Integer categoryId,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size) {
        List<VideoDTO> videos = videoService.getVideosByCategory(categoryId, page, size);
        return Result.success(videos);
    }
    
    /**
     * 获取所有视频标签
     */
    @GetMapping("/tags")
    public Result<List<VideoTag>> getVideoTags() {
        List<VideoTag> tags = videoService.getVideoTags();
        return Result.success(tags);
    }
    
    /**
     * 批量获取视频详情
     */
    @GetMapping("/batch")
    public Result<List<VideoDTO>> getVideosByIds(@RequestParam(value = "ids") List<Long> videoIds) {
        List<VideoDTO> videos = videoService.getVideosByIds(videoIds);
        return Result.success(videos);
    }
    
    /**
     * 审核视频 (模拟后台操作)
     */
    @PostMapping("/{videoId}/audit")
    public Result<Void> auditVideo(@PathVariable Long videoId,
                                    @RequestParam Integer status,
                                    @RequestParam(required = false) String reason) {
        videoService.auditVideo(videoId, status, reason);
        return Result.success();
    }

    /**
     * 获取视频的最新审核失败原因
     */
    @GetMapping("/{videoId}/reject-reason")
    public Result<String> getRejectReason(@PathVariable Long videoId) {
        String reason = videoService.getLatestRejectReason(videoId);
        return Result.success(reason);
    }
}
