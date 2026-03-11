package com.videoplatform.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUrl;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 解析 URL
        String url = elasticsearchUrl;
        String protocol = "http";
        String host = "localhost";
        int port = 9200;

        if (url.startsWith("http://")) {
            protocol = "http";
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            protocol = "https";
            url = url.substring(8);
        }

        String[] parts = url.split(":");
        if (parts.length > 0) {
            host = parts[0];
        }
        if (parts.length > 1) {
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // 使用默认端口
            }
        }

        RestClient restClient = RestClient.builder(
                new HttpHost(host, port, protocol)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }
}
