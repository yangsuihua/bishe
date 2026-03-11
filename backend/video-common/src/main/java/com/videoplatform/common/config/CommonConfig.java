package com.videoplatform.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 全局通用配置类
 */
@Configuration
@MapperScan(basePackages = {"com.videoplatform.**.mapper"})
public class CommonConfig {
}