package com.videoplatform.user.controller;

import com.videoplatform.common.dto.UserProfileVO;
import com.videoplatform.common.dto.UserUpdateDTO;
import com.videoplatform.common.result.Result;
import com.videoplatform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 获取用户信息
     */
    @GetMapping("/profile/{userId}")
    public Result<UserProfileVO> getUserProfile(@PathVariable Long userId) {
        UserProfileVO profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserProfileVO> getCurrentUser(@RequestHeader("X-User-Id") Long userId) {
        UserProfileVO profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Result<Void> updateUser(@RequestHeader("X-User-Id") Long userId, 
                                    @RequestBody UserUpdateDTO dto) {
        userService.updateUser(userId, dto);
        return Result.success();
    }
    
    /**
     * 更新头像
     */
    @PostMapping("/avatar")
    public Result<String> updateAvatar(@RequestHeader("X-User-Id") Long userId,
                                        @RequestParam MultipartFile avatar) {
        String avatarUrl = userService.updateAvatar(userId, avatar);
        return Result.success(avatarUrl);
    }
    
    /**
     * 批量获取用户信息
     */
    @PostMapping("/batch")
    public Result<List<UserProfileVO>> getUsersByIds(@RequestBody List<Long> userIds) {
        List<UserProfileVO> users = userService.getUsersByIds(userIds);
        return Result.success(users);
    }
    
    /**
     * 更新用户获赞数
     */
    @PostMapping("/update-like-count")
    public Result<Void> updateLikeCount(@RequestParam("userId") Long userId,
                                         @RequestParam("delta") Integer delta) {
        userService.updateLikeCount(userId, delta);
        return Result.success();
    }
    
}
