package com.videoplatform.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.videoplatform.common.dto.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final HotSearchService hotSearchService;

    private static final String INDEX_NAME = "videos";

    /**
     * 搜索视频
     * @param keyword 搜索关键词
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 搜索结果列表
     */
    public List<SearchResultDTO> search(String keyword, Integer page, Integer size) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        try {
            // 异步记录搜索关键词（不阻塞主流程）
            hotSearchService.recordSearch(keyword);

            // 构建搜索请求
            int from = (page - 1) * size;
            
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .functionScore(fs -> fs
                                    .query(bq -> bq
                                            .multiMatch(m -> m
                                                    .query(keyword)
                                                    .fields("title^3", "description")
                                                    .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                                            )
                                    )
                                    .functions(f -> f
                                            .fieldValueFactor(fv -> fv
                                                    .field("viewCount")
                                                    .factor(0.1)
                                                    .modifier(co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier.Ln2p)
                                                    .missing(1.0)
                                            )
                                    )
                                    .boostMode(co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode.Multiply)
                            )
                    )
                    .from(from)
                    .size(size)
            );

            // 执行搜索
            SearchResponse<SearchResultDTO> response = elasticsearchClient.search(
                    searchRequest,
                    SearchResultDTO.class
            );

            log.info("搜索关键词 [{}] 完成, 总命中文档数: {}", keyword, response.hits().total() != null ? response.hits().total().value() : 0);

            // 转换结果
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(source -> source != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("搜索失败, keyword: {}", keyword, e);
            // 即使搜索失败，也尝试记录搜索行为（不影响主流程）
            try {
                hotSearchService.recordSearch(keyword);
            } catch (Exception ex) {
                log.error("记录搜索关键词失败", ex);
            }
            return new ArrayList<>();
        }
    }

    /**
     * 搜索视频（带排序）
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @param sortBy 排序字段（viewCount, createdAt）
     * @param sortOrder 排序方向（asc, desc）
     * @return 搜索结果列表
     */
    public List<SearchResultDTO> searchWithSort(String keyword, Integer page, Integer size, 
                                                  String sortBy, String sortOrder) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        try {
            // 异步记录搜索关键词
            hotSearchService.recordSearch(keyword);

            int from = (page - 1) * size;
            boolean ascending = "asc".equalsIgnoreCase(sortOrder);

            SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                    .index(INDEX_NAME)
                    .query(q -> q
                            .multiMatch(m -> m
                                    .query(keyword)
                                    .fields("title^2", "description")
                                    .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
                            )
                    )
                    .from(from)
                    .size(size);

            // 添加排序
            if ("viewCount".equalsIgnoreCase(sortBy)) {
                searchRequestBuilder.sort(s -> s.field(f -> f
                        .field("viewCount")
                        .order(ascending ? 
                                co.elastic.clients.elasticsearch._types.SortOrder.Asc : 
                                co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                ));
            } else if ("createdAt".equalsIgnoreCase(sortBy)) {
                searchRequestBuilder.sort(s -> s.field(f -> f
                        .field("createdAt")
                        .order(ascending ? 
                                co.elastic.clients.elasticsearch._types.SortOrder.Asc : 
                                co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                ));
            }

            SearchRequest searchRequest = searchRequestBuilder.build();

            // 执行搜索
            SearchResponse<SearchResultDTO> response = elasticsearchClient.search(
                    searchRequest,
                    SearchResultDTO.class
            );

            // 转换结果
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(source -> source != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("搜索失败, keyword: {}, sortBy: {}", keyword, sortBy, e);
            // 即使搜索失败，也尝试记录搜索行为
            try {
                hotSearchService.recordSearch(keyword);
            } catch (Exception ex) {
                log.error("记录搜索关键词失败", ex);
            }
            return new ArrayList<>();
        }
    }
    public List<String> getSuggest(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return new ArrayList<>();
        }

        try {
            // 使用 match_phrase_prefix 替代 completion suggester，以支持中段匹配（如“摩天轮”匹配“游乐场的摩天轮”）
            SearchResponse<com.videoplatform.search.entity.VideoDoc> response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .matchPhrasePrefix(mpp -> mpp
                                    .field("title")
                                    .query(prefix)
                            )
                    )
                    .size(10),
                    com.videoplatform.search.entity.VideoDoc.class
            );

            return response.hits().hits().stream()
                    .map(hit -> hit.source().getTitle())
                    .distinct()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取搜索建议失败, prefix: {}", prefix, e);
            return new ArrayList<>();
        }
    }
}
