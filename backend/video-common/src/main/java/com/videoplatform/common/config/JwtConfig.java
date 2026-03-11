package com.videoplatform.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    /**
     * JWT密钥
     */
    private String secret = "video-platform-secret-key-2024-very-long-secret-abcdewdfdsfsdffgsd";
    
    /**
     * Access Token过期时间（秒）
     */
    private Long accessTokenExpiration = 7200L;
    
    /**
     * Refresh Token过期时间（秒）
     */
    private Long refreshTokenExpiration = 604800L;
}