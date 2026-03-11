package com.videoplatform.admin.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.videoplatform.admin.entity.User;
import com.videoplatform.admin.mapper.UserMapper;
import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.result.ResultCode;
import com.videoplatform.admin.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        
        // 白名单路径
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            return true;
        }
        
        // 获取Token
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            writeErrorResponse(response, ResultCode.UNAUTHORIZED.getCode(), "未授权,请先登录");
            return false;
        }
        
        token = token.substring(7);
        
        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            writeErrorResponse(response, ResultCode.TOKEN_INVALID.getCode(), "Token无效或已过期");
            return false;
        }
        
        // 获取用户ID并验证是否为管理员
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        
        if (user == null || user.getDeleted() == 1) {
            writeErrorResponse(response, ResultCode.USER_NOT_EXIST.getCode(), "用户不存在");
            return false;
        }
        
        if (user.getStatus() != null && user.getStatus() == 0) {
            writeErrorResponse(response, ResultCode.USER_DISABLED.getCode(), "用户已被禁用");
            return false;
        }
        
        // 验证是否为管理员
        if (!"admin".equals(user.getRole())) {
            writeErrorResponse(response, ResultCode.FORBIDDEN.getCode(), "无权限访问");
            return false;
        }
        
        // 将用户信息存储到request中
        request.setAttribute("userId", userId);
        request.setAttribute("user", user);
        
        return true;
    }
    
    private void writeErrorResponse(HttpServletResponse response, Integer code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
