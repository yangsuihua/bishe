package com.videoplatform.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotSearchService {

    private final StringRedisTemplate redisTemplate;
    private final KeywordFilterService keywordFilterService;

    private static final String HOT_SEARCH_KEY_PREFIX = "search_hot_rank:";
    private static final String TEMP_KEY_PREFIX = "search_hot_rank:temp:";
    private static final int DEFAULT_TTL_DAYS = 7;
    private static final int TEMP_KEY_TTL_MINUTES = 10;
    
    // 应用层缓存：用于聚合查询结果
    private final Map<String, CacheEntry> aggregationCache = new HashMap<>();
    private static final int CACHE_TTL_MINUTES = 5;

    private static class CacheEntry {
        List<HotSearchItem> data;
        long timestamp;

        CacheEntry(List<HotSearchItem> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MINUTES * 60 * 1000;
        }
    }

    /**
     * 获取今日的 Redis Key
     */
    private String getTodayKey() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return HOT_SEARCH_KEY_PREFIX + dateStr;
    }

    /**
     * 获取指定日期的 Redis Key
     */
    private String getDateKey(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return HOT_SEARCH_KEY_PREFIX + dateStr;
    }

    /**
     * 异步记录搜索关键词
     */
    @Async("hotSearchExecutor")
    public void recordSearch(String keyword) {
        try {
            // 处理关键词：归一化和验证
            String processedKeyword = keywordFilterService.processKeyword(keyword);
            if (processedKeyword == null) {
                log.debug("关键词无效，跳过记录: {}", keyword);
                return;
            }

            String todayKey = getTodayKey();
            
            // 使用 ZINCRBY 增加分数（搜索次数）
            Double newScore = redisTemplate.opsForZSet().incrementScore(todayKey, processedKeyword, 1.0);
            
            // 如果是新创建的 Key，设置 TTL
            if (newScore != null && newScore == 1.0) {
                redisTemplate.expire(todayKey, DEFAULT_TTL_DAYS, TimeUnit.DAYS);
                log.debug("设置热搜 Key TTL: {} 天, key: {}", DEFAULT_TTL_DAYS, todayKey);
            }
            
            log.debug("记录搜索关键词: {}, 当前热度: {}", processedKeyword, newScore);
        } catch (Exception e) {
            log.error("记录搜索关键词失败: {}", keyword, e);
            // 不抛出异常，避免影响主搜索流程
        }
    }

    /**
     * 获取今日热搜 Top N
     */
    public List<HotSearchItem> getTodayHotSearch(int topN) {
        try {
            String todayKey = getTodayKey();
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(todayKey, 0, topN - 1);

            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }

            return tuples.stream()
                    .map(tuple -> new HotSearchItem(
                            tuple.getValue(),
                            tuple.getScore() != null ? tuple.getScore().intValue() : 0
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取今日热搜失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取近 N 天热搜 Top N（使用 ZUNIONSTORE 聚合）
     */
    public List<HotSearchItem> getRecentHotSearch(int days, int topN) {
        // 检查缓存
        String cacheKey = "recent_" + days + "_" + topN;
        CacheEntry cached = aggregationCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("从缓存获取近{}天热搜", days);
            return cached.data;
        }

        try {
            LocalDate today = LocalDate.now();
            List<String> keys = new ArrayList<>();
            
            // 收集近 N 天的 Key
            for (int i = 0; i < days; i++) {
                LocalDate date = today.minusDays(i);
                String key = getDateKey(date);
                // 检查 Key 是否存在
                if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                    keys.add(key);
                }
            }

            if (keys.isEmpty()) {
                return Collections.emptyList();
            }

            // 生成临时 Key
            String tempKey = TEMP_KEY_PREFIX + System.currentTimeMillis();
            
            // 使用 ZUNIONSTORE 合并多个 ZSet
            Long unionSize = redisTemplate.opsForZSet().unionAndStore(
                    tempKey,
                    keys,
                    tempKey
            );

            if (unionSize == null || unionSize == 0) {
                redisTemplate.delete(tempKey);
                return Collections.emptyList();
            }

            // 设置临时 Key 的 TTL
            redisTemplate.expire(tempKey, TEMP_KEY_TTL_MINUTES, TimeUnit.MINUTES);

            // 获取 Top N
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(tempKey, 0, topN - 1);

            List<HotSearchItem> result = Collections.emptyList();
            if (tuples != null && !tuples.isEmpty()) {
                result = tuples.stream()
                        .map(tuple -> new HotSearchItem(
                                tuple.getValue(),
                                tuple.getScore() != null ? tuple.getScore().intValue() : 0
                        ))
                        .collect(Collectors.toList());
            }

            // 更新缓存
            aggregationCache.put(cacheKey, new CacheEntry(result));
            
            // 清理过期缓存
            cleanupExpiredCache();

            return result;
        } catch (Exception e) {
            log.error("获取近{}天热搜失败", days, e);
            return Collections.emptyList();
        }
    }

    /**
     * 清理过期的缓存
     */
    private void cleanupExpiredCache() {
        aggregationCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 热搜项
     */
    public static class HotSearchItem {
        private String keyword;
        private Integer count;

        public HotSearchItem(String keyword, Integer count) {
            this.keyword = keyword;
            this.count = count;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }
}
