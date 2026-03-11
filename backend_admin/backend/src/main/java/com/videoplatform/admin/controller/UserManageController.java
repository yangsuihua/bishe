package com.videoplatform.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.dto.UserQueryDTO;
import com.videoplatform.admin.entity.User;
import com.videoplatform.admin.entity.Video;
import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.service.UserManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManageController {
    
    private final UserManageService userManageService;
    
    @GetMapping
    public Result<Page<User>> getUserList(UserQueryDTO dto) {
        Page<User> result = userManageService.getUserList(dto);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<User> getUserDetail(@PathVariable Long id) {
        User result = userManageService.getUserDetail(id);
        return Result.success(result);
    }
    
    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable Long id, @RequestBody User user) {
        userManageService.updateUser(id, user);
        return Result.success("更新成功");
    }
    
    @PostMapping("/{id}/status")
    public Result<String> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userManageService.updateUserStatus(id, status);
        return Result.success("状态更新成功");
    }
    
    @PostMapping("/{id}/role")
    public Result<String> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        userManageService.updateUserRole(id, role);
        return Result.success("角色更新成功");
    }
    
    @GetMapping("/{id}/videos")
    public Result<Page<Video>> getUserVideos(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Video> result = userManageService.getUserVideos(id, page, size);
        return Result.success(result);
    }
}
