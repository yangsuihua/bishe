package com.videoplatform.user.controller;

import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.result.Result;
import com.videoplatform.user.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关注控制器
 */
@RestController
@RequestMapping("/user/follow")
@RequiredArgsConstructor
public class FollowController {
    
    private final FollowService followService;
    
    /**
     * 关注用户
     */
    @PostMapping("/{targetUserId}")
    public Result<Void> followUser(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long targetUserId) {
        followService.followUser(userId, targetUserId);
        return Result.success();
    }
    
    /**
     * 取消关注
     */
    @DeleteMapping("/{targetUserId}")
    public Result<Void> unfollowUser(@RequestHeader("X-User-Id") Long userId,
                                      @PathVariable Long targetUserId) {
        followService.unfollowUser(userId, targetUserId);
        return Result.success();
    }
    
    /**
     * 检查是否关注
     */
    @GetMapping("/check/{targetUserId}")
    public Result<Boolean> isFollowing(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long targetUserId) {
        boolean isFollowing = followService.isFollowing(userId, targetUserId);
        return Result.success(isFollowing);
    }
    
    /**
     * 获取关注列表
     */
    @GetMapping("/following")
    public Result<List<UserProfileVO>> getFollowingList(@RequestHeader("X-User-Id") Long userId) {
        List<UserProfileVO> list = followService.getFollowingList(userId);
        return Result.success(list);
    }
    
    @GetMapping("/following/{targetUserId}")
    public Result<List<UserProfileVO>> getFollowingListByUserId(@PathVariable Long targetUserId) {
        List<UserProfileVO> list = followService.getFollowingList(targetUserId);
        return Result.success(list);
    }
    
    /**
     * 获取粉丝列表
     */
    @GetMapping("/followers")
    public Result<List<UserProfileVO>> getFollowersList(@RequestHeader("X-User-Id") Long userId) {
        List<UserProfileVO> list = followService.getFollowersList(userId);
        return Result.success(list);
    }
    
    @GetMapping("/followers/{targetUserId}")
    public Result<List<UserProfileVO>> getFollowersListByUserId(@PathVariable Long targetUserId) {
        List<UserProfileVO> list = followService.getFollowersList(targetUserId);
        return Result.success(list);
    }
}
