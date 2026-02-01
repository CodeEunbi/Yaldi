package com.yaldi.infra.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 클라이언트 설정
 *
 * 문제: ES Java Client의 기본 ObjectMapper는 Java 8 날짜/시간 타입(OffsetDateTime 등)을 처리하지 못함
 * 해결: JavaTimeModule을 등록한 커스텀 ObjectMapper를 사용하도록 설정
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // HttpHost 파싱
        HttpHost host = HttpHost.create(elasticsearchUri);

        // RestClient 생성
        RestClient restClient = RestClient.builder(host).build();

        // JavaTimeModule이 등록된 ObjectMapper 생성
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // JacksonJsonpMapper에 커스텀 ObjectMapper 주입
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);

        // RestClientTransport 생성
        RestClientTransport transport = new RestClientTransport(restClient, jsonpMapper);

        // ElasticsearchClient 생성
        return new ElasticsearchClient(transport);
    }
}
