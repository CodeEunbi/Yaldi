package com.yaldi.domain.version.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Graph RAG (Neo4j) 인덱싱 클라이언트
 * 빌드 성공한 프로젝트를 Neo4j에 인덱싱하여 향후 AI가 참고할 수 있도록 함
 */
@Slf4j
@Component
public class GraphRagAiClient {

    private final WebClient webClient;
    private final long timeout;

    public GraphRagAiClient(
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.server.timeout:30000}") long timeout
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.timeout = timeout;
        log.info("GraphRagAiClient initialized - URL: {}", aiServerUrl);
    }

    public boolean indexToGraph(
            Long versionKey,
            String versionName,
            String versionDescription,
            String projectName,
            String projectDescription,
            Map<String, Object> schemaData,
            Boolean isPublic,
            String designVerificationStatus
    ) {
        try {
            log.info("Graph RAG 인덱싱 요청 - versionKey: {}, project: {}, version: {}",
                    versionKey, projectName, versionName);

            // Request Body 생성
            Map<String, Object> requestBody = Map.of(
                    "version_key", versionKey,
                    "version_name", versionName,
                    "version_description", versionDescription != null ? versionDescription : "",
                    "project_name", projectName,
                    "project_description", projectDescription != null ? projectDescription : "",
                    "schema_data", schemaData,
                    "is_public", isPublic,
                    "design_verification_status", designVerificationStatus
            );

            // AI 서버 호출 (POST /api/v1/graph-rag/index)
            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/graph-rag/index")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofMillis(timeout));

            if (response == null) {
                log.error("Graph RAG 인덱싱 응답 없음 - versionKey: {}", versionKey);
                return false;
            }

            Boolean success = (Boolean) response.getOrDefault("success", false);

            if (success) {
                log.info("Graph RAG 인덱싱 완료 - versionKey: {}, project: {}", versionKey, projectName);
            } else {
                log.error("Graph RAG 인덱싱 실패 - versionKey: {}, response: {}", versionKey, response);
            }

            return success;

        } catch (WebClientResponseException e) {
            log.error("Graph RAG 인덱싱 API 호출 실패 - versionKey: {}, Status: {}, Body: {}",
                    versionKey, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return false;
        } catch (Exception e) {
            log.error("Graph RAG 인덱싱 중 예외 발생 - versionKey: {}", versionKey, e);
            return false;
        }
    }
}
