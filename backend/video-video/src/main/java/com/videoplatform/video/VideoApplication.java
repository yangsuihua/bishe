package com.videoplatform.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
// 关键点：必须指定 basePackages，指向 common 模块的包名
@EnableFeignClients(basePackages = "com.videoplatform.common.feign")
@MapperScan("com.videoplatform.video.mapper")
public class VideoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(VideoApplication.class, args);
        System.out.println("========== 视频服务启动成功 ==========");
    }
}
