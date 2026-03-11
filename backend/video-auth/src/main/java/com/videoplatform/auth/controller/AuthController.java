package com.videoplatform.auth.controller;

import com.videoplatform.common.dto.LoginDTO;
import com.videoplatform.common.dto.RegisterDTO;
import com.videoplatform.common.dto.TokenResponse;
import com.videoplatform.auth.service.AuthService;
import com.videoplatform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<TokenResponse> login(@RequestBody LoginDTO dto) {
        TokenResponse response = authService.login(dto);
        System.out.println("登录成功"+response);
        return Result.success(response);
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<TokenResponse> register(@RequestBody RegisterDTO dto) {
        TokenResponse response = authService.register(dto);
        return Result.success(response);
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId) {
        authService.logout(userId);
        return Result.success();
    }
    
    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@RequestBody String refreshToken) {
        TokenResponse response = authService.refreshToken(refreshToken);
        return Result.success(response);
    }
}
