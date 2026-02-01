package com.yaldi.domain.version.client;

import com.yaldi.domain.version.dto.response.VersionVerificationResult;
import com.yaldi.domain.version.entity.DesignVerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class VersionAiClient {

    // WebClient는 비동기·논블로킹 방식으로 동시 요청 처리 효율이 높아,
    // 외부 API 호출이 많은 환경에 더 적합하기 때문에 RestTemplate 방식이 아닌 WebClient 방식을 사용

    private final WebClient webClient;
    private final long timeout;
    private final ObjectMapper objectMapper;

    public VersionAiClient(
            @Value("${ai.server.url}") String aiServerUrl,
            @Value("${ai.server.timeout:300000}") long timeout,
            ObjectMapper objectMapper
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.timeout = timeout;
        this.objectMapper = objectMapper;
    }

    public String createSql (Map<String, Object> schemaData, Integer rowCount) {

        try {
            // Request Body 생성
            Map<String, Object> requestBody = Map.of(
                    "schemaData", schemaData,
                    "rowCount", rowCount
            );

            // AI 서버 호출 (POST /api/v1/version/mock-data)
            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/version/mock-data")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofMillis(timeout));

            if (response == null || !response.containsKey("sql")) {
                throw new RuntimeException("AI 서버 응답이 올바르지 않습니다: sql 필드 없음");
            }

            String sql = (String) response.get("sql");
            log.info("AI 서버 Mock SQL 생성 완료 - SQL 길이: {} bytes", sql.length());
            return sql;

        } catch (WebClientResponseException e) {
            log.error("AI 서버 호출 실패 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("AI 서버 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI 서버 호출 중 예외 발생", e);
            throw new RuntimeException("AI 서버 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public VersionVerificationResult verifySchema(Map<String, Object> schemaData, String versionName) {

        try {
            log.info("AI 서버에 스키마 검증 요청 - Version: {}", versionName);

            // Request Body 생성
            Map<String, Object> requestBody = Map.of(
                    "schemaData", schemaData,
                    "versionName", versionName
            );

            // AI 서버 호출 (POST /api/v1/version/verification)
            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/version/verification")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofMillis(timeout));

            if (response == null) {
                throw new RuntimeException("AI 서버 응답이 올바르지 않습니다: 응답 없음");
            }

            // 응답 파싱
            Boolean isValid = (Boolean) response.getOrDefault("isValid", false);
            String statusStr = (String) response.getOrDefault("status", "FAILED");
            List<String> errors = (List<String>) response.getOrDefault("errors", List.of());
            List<String> warnings = (List<String>) response.getOrDefault("warnings", List.of());
            String message = (String) response.getOrDefault("message", "검증 완료");
            List<String> suggestions = (List<String>) response.getOrDefault("suggestions", List.of());

            // DesignVerificationStatus 변환
            DesignVerificationStatus status;
            try {
                status = DesignVerificationStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 검증 상태: {}, FAILED로 처리", statusStr);
                status = DesignVerificationStatus.FAILED;
            }

            log.info("AI 서버 스키마 검증 완료 - Version: {}, Status: {}, IsValid: {}",
                    versionName, status, isValid);

            return new VersionVerificationResult(isValid, status, errors, warnings, message, suggestions);

        } catch (WebClientResponseException e) {
            log.error("AI 서버 호출 실패 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("AI 서버 호출 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI 서버 호출 중 예외 발생", e);
            throw new RuntimeException("AI 서버 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public List<Double> generateEmbedding(Long versionId, Long projectId, String projectName, String projectDescription, String versionName, String versionDescription, Map<String, Object> schemaData ) {
        try {
            log.info("AI 서버에 임베딩 생성 요청 - Version: {}", versionName);

            // schemaData를 JSON 문자열로 변환 (sqlContent)
            String sqlContent = objectMapper.writeValueAsString(schemaData);

            // Request Body 생성 (AI 서버 형식에 맞춤)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("versionId", versionId);
            requestBody.put("projectId", projectId);
            requestBody.put("projectName", projectName);
            requestBody.put("projectDescription", projectDescription);
            requestBody.put("versionName", versionName);
            requestBody.put("versionDescription", versionDescription);
            requestBody.put("sqlContent", sqlContent);

            // AI 서버 호출 (POST /api/v1/version/embedding)
            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/version/embedding")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofMillis(timeout));

            if (response == null || !response.containsKey("vector")) {
                throw new RuntimeException("AI 서버 응답이 올바르지 않습니다: vector 필드 없음");
            }

            List<Double> vector = (List<Double>) response.get("vector");

            log.info("AI 서버 임베딩 생성 완료 - Version: {}, Vector dimension: {}",
                    versionName, vector.size());

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
