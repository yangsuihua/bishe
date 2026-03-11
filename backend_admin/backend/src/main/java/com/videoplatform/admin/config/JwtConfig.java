package com.videoplatform.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret = "video-platform-secret-key-2024-very-long-secret-abcdewdfdsfsdffgsd";
    private Long accessTokenExpiration = 1209600L; // 14天
    private Long refreshTokenExpiration = 604800L; // 7天
}
