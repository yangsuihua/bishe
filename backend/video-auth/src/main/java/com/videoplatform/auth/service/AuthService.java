package com.videoplatform.auth.service;

import com.videoplatform.common.dto.LoginDTO;
import com.videoplatform.common.dto.RegisterDTO;
import com.videoplatform.common.dto.TokenResponse;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    TokenResponse login(LoginDTO dto);
    
    /**
     * 用户注册
     */
    TokenResponse register(RegisterDTO dto);
    
    /**
     * 退出登录
     */
    void logout(Long userId);
    
    /**
     * 刷新Token
     */
    TokenResponse refreshToken(String refreshToken);
}