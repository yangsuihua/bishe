package com.videoplatform.interaction.controller;

import com.videoplatform.common.entity.UserLike;
import com.videoplatform.common.result.Result;
import com.videoplatform.interaction.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 点赞控制器
 */
@RestController
@RequestMapping("/interaction/like")
@RequiredArgsConstructor
public class LikeController {
    
    private final LikeService likeService;
    
    /**
     * 点赞视频
     */
    @PostMapping("/{videoId}")
    public Result<Void> likeVideo(@RequestHeader("X-User-Id") Long userId,
                                   @PathVariable Long videoId) {
        likeService.likeVideo(userId, videoId);
        return Result.success();
    }
    
    /**
     * 取消点赞
     */
    @DeleteMapping("/{videoId}")
    public Result<Void> unlikeVideo(@RequestHeader("X-User-Id") Long userId,
                                     @PathVariable Long videoId) {
        likeService.unlikeVideo(userId, videoId);
        return Result.success();
    }
    
    /**
     * 检查是否点赞
     */
    @GetMapping("/check/{videoId}")
    public Result<Boolean> checkLiked(@RequestHeader("X-User-Id") Long userId,
                                       @PathVariable Long videoId) {
        boolean isLiked = likeService.isLiked(userId, videoId);
        return Result.success(isLiked);
    }
    
    /**
     * 获取用户点赞的视频列表
     */
    @GetMapping("/user")
    public Result<List<UserLike>> getUserLikedVideos(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        List<UserLike> likes = likeService.listLikedRecords(userId, page, size);
        return Result.success(likes);
    }
    
    @GetMapping("/user/{targetUserId}")
    public Result<List<UserLike>> getUserLikedVideosByUserId(@PathVariable Long targetUserId,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        List<UserLike> likes = likeService.listLikedRecords(targetUserId, page, size);
        return Result.success(likes);
    }
}