package com.videoplatform.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@org.mybatis.spring.annotation.MapperScan("com.videoplatform.search.mapper")
@EnableDiscoveryClient
// 关键点：必须指定 basePackages，指向 common 模块的包名
@EnableFeignClients(basePackages = "com.videoplatform.common.feign")
public class SearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
    }
}