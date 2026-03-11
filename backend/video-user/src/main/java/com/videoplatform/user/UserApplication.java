package com.videoplatform.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 用户服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
// 关键点：必须指定 basePackages，指向 common 模块的包名
@EnableFeignClients(basePackages = "com.videoplatform.common.feign")
@MapperScan("com.videoplatform.user.mapper")
public class UserApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
        System.out.println("========== 用户服务启动成功 ==========");
    }
}
