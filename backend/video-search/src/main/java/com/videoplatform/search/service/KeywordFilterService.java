package com.videoplatform.search.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class KeywordFilterService {

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_KEYWORD_LENGTH = 50;
    
    // 违禁词列表（实际项目中可以从配置文件或数据库加载）
    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(Arrays.asList(
        "test", "admin", "system"
    ));

    /**
     * 归一化关键词
     * 去除首尾空格，转小写
     */
    public String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }

    /**
     * 验证关键词是否有效
     * @param keyword 关键词
     * @return true 如果关键词有效，false 如果无效
     */
    public boolean validateKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }

        String normalized = normalizeKeyword(keyword);
        
        // 检查长度
        if (normalized.length() < MIN_KEYWORD_LENGTH) {
            log.debug("关键词过短: {}", keyword);
            return false;
        }
        
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            log.debug("关键词过长: {}", keyword);
            return false;
        }

        // 检查违禁词
        if (FORBIDDEN_KEYWORDS.contains(normalized)) {
            log.debug("关键词为违禁词: {}", keyword);
            return false;
        }

        // 检查是否只包含空白字符
        if (normalized.trim().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * 处理关键词：归一化并验证
     * @param keyword 原始关键词
     * @return 归一化后的关键词，如果无效则返回 null
     */
    public String processKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        
        String normalized = normalizeKeyword(keyword);
        
        if (validateKeyword(normalized)) {
            return normalized;
        }
        
        return null;
    }
}
