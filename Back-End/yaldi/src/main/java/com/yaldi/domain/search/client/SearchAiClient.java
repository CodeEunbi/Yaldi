package com.yaldi.domain.search.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SearchAiClient {

    private final WebClient webClient;
    private final long timeout;

    public SearchAiClient(
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.server.timeout:300000}") long timeout
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.timeout = timeout;
    }

    public List<Double> generateSearchEmbedding(String queryText) {
        try {
            log.info("AI 서버에 검색 쿼리 임베딩 생성 요청 - Query: {}", queryText);

            // Request Body 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", queryText);

            // AI 서버 호출 (POST /api/v1/search/embedding)
            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/search/embedding")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofMillis(timeout));

            if (response == null || !response.containsKey("queryVector")) {
                throw new RuntimeException("AI 서버 응답이 올바르지 않습니다: queryVector 필드 없음");
            }

            List<Double> vector = (List<Double>) response.get("queryVector");

            log.info("AI 서버 검색 쿼리 임베딩 생성 완료 - Query: {}, Vector dimension: {}", queryText, vector.size());
            return vector;

        } catch (WebClientResponseException e) {
            log.error("AI 서버 호출 실패 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("AI 서버 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI 서버 호출 중 예외 발생", e);
            throw new RuntimeException("AI 서버 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
