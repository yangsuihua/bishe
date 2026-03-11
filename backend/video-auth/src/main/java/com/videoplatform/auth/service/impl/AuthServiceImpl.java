package com.videoplatform.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.common.dto.LoginDTO;
import com.videoplatform.common.dto.RegisterDTO;
import com.videoplatform.common.dto.TokenResponse;
import com.videoplatform.auth.service.AuthService;
import com.videoplatform.common.exception.BusinessException;
import com.videoplatform.common.result.ResultCode;
import com.videoplatform.common.utils.JwtUtil;
import com.videoplatform.common.entity.User;
import com.videoplatform.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public TokenResponse login(LoginDTO dto) {
        // 根据用户名查找用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        
        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        
        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 生成Token
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        return new TokenResponse(accessToken, refreshToken, user.getId(), user.getUsername(), 
                jwtUtil.getJwtConfig().getAccessTokenExpiration());
    }
    
    @Override
    public TokenResponse register(RegisterDTO dto) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (existingUser != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        
        // 检查邮箱是否已存在
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, dto.getEmail()));
            if (existingUser != null) {
                throw new BusinessException(ResultCode.EMAIL_EXISTS);
            }
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getUsername()); // 默认昵称为用户名
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(1); // 默认启用
        user.setRole("user"); // 默认角色
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setLikeCount(0L);
        user.setVideoCount(0);
        user.setIsFavoritesVisible(1); // 默认收藏列表公开
        user.setIsLikesVisible(0);     // 默认点赞列表私密
        
        userMapper.insert(user);
        
        // 生成Token
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        return new TokenResponse(accessToken, refreshToken, user.getId(), user.getUsername(), 
                jwtUtil.getJwtConfig().getAccessTokenExpiration());
    }
    
    @Override
    public void logout(Long userId) {
        // 登出操作通常在前端清除Token即可，后端无需特殊处理
        log.info("用户 {} 已登出", userId);
    }
    
    @Override
    public TokenResponse refreshToken(String refreshToken) {
        // 验证Refresh Token
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        
        // 从Token中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        
        // 查找用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        
        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        
        // 生成新的Access Token
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        return new TokenResponse(newAccessToken, refreshToken, user.getId(), user.getUsername(), 
                jwtUtil.getJwtConfig().getAccessTokenExpiration());
    }
}