package com.videoplatform.common.feign;

import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.entity.User;
import com.videoplatform.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 */
// contextId 避免与其它 video-user FeignClient 冲突
@FeignClient(name = "video-user", contextId = "videoUserClient", path = "/user")
public interface UserFeignClient {
    
    /**
     * 获取用户信息
     */
    @GetMapping("/profile/{userId}")
    Result<UserProfileVO> getUserProfile(@PathVariable("userId") Long userId);
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    Result<UserProfileVO> getCurrentUser(@RequestHeader("X-User-Id") Long userId);
    
    /**
     * 批量获取用户信息
     */
    @PostMapping("/batch")
    Result<List<UserProfileVO>> getUsersByIds(@RequestBody List<Long> userIds);
    
    /**
     * 更新用户获赞数
     */
    @PostMapping("/update-like-count")
    Result<Void> updateLikeCount(@RequestParam("userId") Long userId, 
                                  @RequestParam("delta") Integer delta);

    /**
     * 取消关注
     */
    @DeleteMapping("/follow/{targetUserId}")
    Result<Void> unfollowUser(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("targetUserId") Long targetUserId);
}
