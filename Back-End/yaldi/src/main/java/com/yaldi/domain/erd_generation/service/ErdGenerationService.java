package com.yaldi.domain.erd_generation.service;

import com.yaldi.domain.erd_generation.client.ErdGenerationAiClient;
import com.yaldi.domain.erd_generation.dto.request.ErdGenerationRequest;
import com.yaldi.domain.erd_generation.dto.response.ErdGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

//AI 서버의 Multi-Agent + Graph RAG + LangGraph 기반 ERD 생성 기능 연동
@Slf4j
@Service
@RequiredArgsConstructor
public class ErdGenerationService {

    private final ErdGenerationAiClient erdGenerationAiClient;

    public ErdGenerationResponse generateErd(ErdGenerationRequest request) {
        log.info("ERD 생성 요청 - Project: {}", request.projectName());

        try {
            // AI 서버 호출
            ErdGenerationResponse response = erdGenerationAiClient.generateErd(
                    request.projectName(),
                    request.projectDescription(),
                    request.userPrompt()
            );

            log.info("ERD 생성 완료 - Project: {}, Mode: {}, Confidence: {}, Time: {}ms",
                    request.projectName(),
                    response.getMode(),
                    response.getConfidenceScore(),
                    response.getExecutionTimeMs());

            return response;

        } catch (Exception e) {
            log.error("ERD 생성 실패 - Project: {}", request.projectName(), e);
            throw new RuntimeException("ERD 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
