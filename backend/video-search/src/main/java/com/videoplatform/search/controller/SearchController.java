package com.videoplatform.search.controller;

import com.videoplatform.common.dto.SearchResultDTO;
import com.videoplatform.common.result.Result;
import com.videoplatform.search.dto.HotSearchResponse;
import com.videoplatform.search.service.HotSearchService;
import com.videoplatform.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final HotSearchService hotSearchService;

    /**
     * 搜索接口
     * @param keyword 搜索关键词
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     * @param sortBy 排序字段（viewCount, createdAt）
     * @param sortOrder 排序方向（asc, desc）
     * @return 搜索结果
     */
    @GetMapping
    public Result<List<SearchResultDTO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        if (!StringUtils.hasText(keyword)) {
            return Result.fail("搜索关键词不能为空");
        }

        try {
            List<SearchResultDTO> results;
            if (StringUtils.hasText(sortBy)) {
                results = searchService.searchWithSort(keyword, page, size, sortBy, sortOrder);
            } else {
                results = searchService.search(keyword, page, size);
            }
            return Result.success(results);
        } catch (Exception e) {
            log.error("搜索失败, keyword: {}", keyword, e);
            return Result.fail("搜索失败，请稍后重试");
        }
    }

    /**
     * 获取热搜榜
     * @param type 类型：today（今日热搜）或 recent（近N天热搜）
     * @param days 近N天（仅当type为recent时有效，默认3）
     * @param top 返回Top N（默认10）
     * @return 热搜榜
     */
    @GetMapping("/hot")
    public Result<HotSearchResponse> getHotSearch(
            @RequestParam(defaultValue = "today") String type,
            @RequestParam(defaultValue = "3") Integer days,
            @RequestParam(defaultValue = "10") Integer top
    ) {
        try {
            List<HotSearchService.HotSearchItem> items;
            
            if ("recent".equalsIgnoreCase(type)) {
                items = hotSearchService.getRecentHotSearch(days, top);
            } else {
                // 默认今日热搜
                items = hotSearchService.getTodayHotSearch(top);
            }

            // 转换为响应DTO
            List<HotSearchResponse.HotSearchItem> responseItems = items.stream()
                    .map(item -> new HotSearchResponse.HotSearchItem(
                            item.getKeyword(),
                            item.getCount()
                    ))
                    .collect(Collectors.toList());

            HotSearchResponse response = new HotSearchResponse();
            response.setType(type);
            if ("recent".equalsIgnoreCase(type)) {
                response.setDays(days);
            }
            response.setItems(responseItems);

            return Result.success(response);
        } catch (Exception e) {
            log.error("获取热搜榜失败, type: {}, days: {}", type, days, e);
            return Result.fail("获取热搜榜失败，请稍后重试");
        }
    }
}
