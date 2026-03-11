package com.videoplatform.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.admin.dto.LoginDTO;
import com.videoplatform.admin.entity.User;
import com.videoplatform.admin.mapper.UserMapper;
import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.result.ResultCode;
import com.videoplatform.admin.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        
        if (user == null || user.getDeleted() == 1) {
            return Result.fail(ResultCode.USERNAME_OR_PASSWORD_ERROR.getCode(), 
                    ResultCode.USERNAME_OR_PASSWORD_ERROR.getMessage());
        }
        
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.fail(ResultCode.USERNAME_OR_PASSWORD_ERROR.getCode(), 
                    ResultCode.USERNAME_OR_PASSWORD_ERROR.getMessage());
        }
        
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail(ResultCode.USER_DISABLED.getCode(), 
                    ResultCode.USER_DISABLED.getMessage());
        }
        
        // 验证是否为管理员
        if (!"admin".equals(user.getRole())) {
            return Result.fail(ResultCode.FORBIDDEN.getCode(), "该账户不是管理员");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        
        return Result.success(data);
    }
}
