package com.yaldi.domain.erd_generation.client;

import com.yaldi.domain.erd_generation.dto.response.ErdGenerationResponse;
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
 * AI 서버 ERD 자동 생성 API 클라이언트
 * Multi-Agent + Graph RAG + LangGraph 기반 ERD 생성
 */
@Slf4j
@Component
public class ErdGenerationAiClient {

    private final WebClient webClient;
    private final long timeout;

    public ErdGenerationAiClient(
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.server.erd-generation.timeout:300000}") long timeout
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.timeout = timeout;
        log.info("ErdGenerationAiClient initialized - URL: {}, Timeout: {}ms", aiServerUrl, timeout);
    }

    public ErdGenerationResponse generateErd(
            String projectName,
            String projectDescription,
            String userPrompt
    ) {
        try {
            log.info("AI 서버에 ERD 생성 요청 - Project: {}", projectName);

            // Request Body 생성
            Map<String, Object> requestBody = Map.of(
                    "project_name", projectName,
                    "project_description", projectDescription != null ? projectDescription : "",
                    "user_prompt", userPrompt
            );

            // AI 서버 호출 (POST /api/v1/erd/generate)
            ErdGenerationResponse response = webClient.post()
                    .uri("/api/v1/erd/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ErdGenerationResponse.class)
                    .block(Duration.ofMillis(timeout));

            if (response == null) throw new RuntimeException("AI 서버 응답이 올바르지 않습니다: 응답 없음");

            log.info("AI 서버 ERD 생성 완료 - Project: {}, Mode: {}, ExecutionTime: {}ms", projectName, response.getMode(), response.getExecutionTimeMs());
            return response;

        } catch (WebClientResponseException e) {
            log.error("AI 서버 호출 실패 - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("AI 서버 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI 서버 호출 중 예외 발생", e);
            throw new RuntimeException("AI 서버 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
