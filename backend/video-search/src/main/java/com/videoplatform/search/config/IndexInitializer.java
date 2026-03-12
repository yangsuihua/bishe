package com.videoplatform.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitializer implements CommandLineRunner {

    private final ElasticsearchClient client;
    private static final String INDEX_NAME = "videos";

    @Override
    public void run(String... args) throws Exception {
        try {
            // 1. 检查索引是否存在
            BooleanResponse exists = client.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME)));
            
            if (!exists.value()) {
                log.info("ES 索引 {} 不存在，正在创建...", INDEX_NAME);
                
                // 2. 创建索引并定义 Mapping
                CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .mappings(m -> m
                        .properties("id", p -> p.long_(l -> l))
                        .properties("userId", p -> p.long_(l -> l))
                        .properties("title", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                        .properties("description", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                        .properties("tags", p -> p.keyword(k -> k))
                        .properties("viewCount", p -> p.long_(l -> l))
                        .properties("publishedAt", p -> p.date(d -> d))
                        .properties("coverUrl", p -> p.keyword(k -> k))
                        // 核心：下拉补全字段
                        .properties("suggestion", p -> p.completion(cp -> cp))
                    )
                );

                client.indices().create(request);
                log.info("ES 索引 {} 创建成功，Mapping 已定义。", INDEX_NAME);
            } else {
                log.info("ES 索引 {} 已存在，跳过初始化。", INDEX_NAME);
            }
        } catch (Exception e) {
            log.error("ES 索引初始化失败: {}", e.getMessage());
            // 注意：针对没有安装 IK 分词器的环境，如果报错，建议手动降级为 standard 分词器
        }
    }
}
