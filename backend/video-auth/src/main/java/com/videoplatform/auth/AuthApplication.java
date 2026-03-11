package com.videoplatform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// 仅扫描认证模块与 common 模块，避免把其它微服务的配置一起扫进来
@SpringBootApplication(scanBasePackages = {"com.videoplatform.auth", "com.videoplatform.common"})
@EnableDiscoveryClient
// 关键点：必须指定 basePackages，指向 common 模块的包名
@EnableFeignClients(basePackages = "com.videoplatform.common.feign")
@EnableConfigurationProperties
public class AuthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("========== 认证服务启动成功 ==========");
    }
}
