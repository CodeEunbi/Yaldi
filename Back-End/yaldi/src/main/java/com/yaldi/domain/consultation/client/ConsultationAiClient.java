package com.yaldi.domain.consultation.client;

import com.yaldi.domain.consultation.dto.request.ConsultationAiRequest;
import com.yaldi.domain.consultation.dto.response.ConsultationAiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * AI 서버 ERD 상담 챗봇 API 클라이언트
 * Multi-Agent + Intent Routing + LangGraph 기반 상담
 */
@Slf4j
@Component
public class ConsultationAiClient {

    private final WebClient webClient;
    private final long timeout;

    public ConsultationAiClient(
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.server.consultation.timeout:300000}") long timeout
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.timeout = timeout;
        log.info("ConsultationAiClient initialized - URL: {}, Timeout: {}ms", aiServerUrl, timeout);
    }

    /**
     * AI 서버에 상담 요청 전송
     *
     * @param request 상담 요청 (질문 + 스키마 + 대화 히스토리)
     * @return AI 응답 (답변 + 수정 제안 + 확신도)
     */
    public ConsultationAiResponse consult(ConsultationAiRequest request) {
        try {
            log.info("AI 서버에 상담 요청 - ProjectKey: {}, Message: {}",
                    request.getProjectKey(), request.getMessage());

            // AI 서버 호출 (POST /api/v1/consultation/consult)
            ConsultationAiResponse response = webClient.post()
                    .uri("/api/v1/consultation/consult")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ConsultationAiResponse.class)
                    .block(Duration.ofMillis(timeout));

            if (response == null) {
                throw new RuntimeException("AI 서버 응답이 올바르지 않습니다: 응답 없음");
            }

            log.info("AI 서버 상담 완료 - ProjectKey: {}, Confidence: {}, AgentsUsed: {}",
                    request.getProjectKey(), response.getConfidence(), response.getAgentsUsed());

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
