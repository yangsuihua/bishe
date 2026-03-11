package com.videoplatform.interaction.controller;

import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.feign.UserFeignClient;
import com.videoplatform.common.result.Result;
import com.videoplatform.interaction.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/interaction/friendship")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final UserFeignClient userFeignClient;

    @PostMapping
    public Result<Boolean> addFriend(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestParam Long targetUserId) {
        boolean success = friendshipService.addFriendship(currentUserId, targetUserId);
        return success ? Result.success(true) : Result.fail("Failed to add friendship");
    }

    @DeleteMapping("/{targetUserId}")
    public Result<Void> deleteFriend(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable Long targetUserId) {
        // 步骤1: 删除好友关系
        friendshipService.removeFriendship(currentUserId, targetUserId);

        // 步骤2: 异步删除关注关系（取关），避免阻塞
        // 注意：这里只删除当前用户对目标用户的关注，不删除反向关注
        // 因为删除好友不应该影响目标用户对当前用户的关注状态
        new Thread(() -> {
            try {
                userFeignClient.unfollowUser(currentUserId, targetUserId);
            } catch (Exception e) {
                // 如果取关失败（可能本来就没关注），忽略错误
                // 不记录日志，避免日志污染
            }
        }).start();
        
        return Result.success();
    }

    @GetMapping("/check")
    public Result<Boolean> checkFriendship(
            @RequestHeader("X-User-Id") Long currentUserId,
            @RequestParam Long targetUserId) {
        boolean isFriend = friendshipService.isFriends(currentUserId, targetUserId);
        return Result.success(isFriend);
    }

    @GetMapping("/list")
    public Result<List<com.videoplatform.common.entity.Friendship>> getFriends(
            @RequestHeader("X-User-Id") Long currentUserId) {
        List<com.videoplatform.common.entity.Friendship> friends = friendshipService.getFriends(currentUserId);
        return Result.success(friends);
    }
    
    @GetMapping("/friend-list")
    public Result<List<UserProfileVO>> getFriendList(
            @RequestHeader("X-User-Id") Long currentUserId) {
        List<UserProfileVO> friendList = friendshipService.getFriendList(currentUserId);
        return Result.success(friendList);
    }
}