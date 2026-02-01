package com.yaldi.domain.consultation.service;

import com.yaldi.domain.consultation.client.ConsultationAiClient;
import com.yaldi.domain.consultation.entity.ConsultationConverter;
import com.yaldi.domain.consultation.dto.request.ConsultationAiRequest;
import com.yaldi.domain.consultation.dto.response.ConsultationAiResponse;
import com.yaldi.domain.consultation.dto.response.ConsultationHistoryResponse;
import com.yaldi.domain.consultation.dto.response.ConsultationMessageResponse;
import com.yaldi.domain.consultation.dto.request.SendConsultationRequest;
import com.yaldi.domain.consultation.entity.ConsultationMessage;
import com.yaldi.domain.consultation.entity.ConsultationMessageRole;
import com.yaldi.domain.consultation.repository.ConsultationMessageRepository;
import com.yaldi.domain.consultation.validator.ConsultationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationMessageRepository consultationMessageRepository;
    private final ConsultationValidator consultationValidator;
    private final ConsultationAiClient consultationAiClient;

    @Transactional
    public ConsultationMessageResponse sendMessage(Integer userKey, SendConsultationRequest request) {
        // 프로젝트 검증 (존재 여부, 삭제 여부, 접근 권한)
        consultationValidator.validateProjectAccess(request.projectKey(), userKey);

        ConsultationMessage userMessage = ConsultationMessage.builder()
                .projectKey(request.projectKey())
                .role(ConsultationMessageRole.USER)
                .message(request.message())
                .schemaSnapshot(request.schemaData()) // 현재 스키마 스냅샷 저장
                .build();

        consultationMessageRepository.save(userMessage);
        log.info("User message saved: projectKey={}, messageKey={}", request.projectKey(), userMessage.getMessageKey());

        // 최근 대화 히스토리 조회 (AI에게 보낼 컨텍스트)
        List<ConsultationMessage> recentMessages = consultationMessageRepository
                .findTop20ByProjectKeyOrderByCreatedAtDesc(request.projectKey());
        Collections.reverse(recentMessages); // 시간순으로 변경

        // AI 요청 빌드
        ConsultationAiRequest aiRequest = buildAiRequest(request, recentMessages);

        // AI 서비스 호출
        ConsultationAiResponse aiResponse = consultationAiClient.consult(aiRequest);

        // AI 응답 저장
        ConsultationMessage assistantMessage = ConsultationMessage.builder()
                .projectKey(request.projectKey())
                .role(ConsultationMessageRole.ASSISTANT)
                .message(aiResponse.getMessage())
                .schemaModifications(aiResponse.getSchemaModifications())
                .confidence(aiResponse.getConfidence())
                .agentsUsed(aiResponse.getAgentsUsed())
                .warnings(aiResponse.getWarnings())
                .build();

        consultationMessageRepository.save(assistantMessage);
        log.info("Assistant message saved: projectKey={}, messageKey={}", request.projectKey(), assistantMessage.getMessageKey());

        return ConsultationConverter.toResponse(assistantMessage);
    }

    @Transactional(readOnly = true)
    public ConsultationHistoryResponse getHistory(Integer userKey, Long projectKey) {
        // 프로젝트 검증 (존재 여부, 삭제 여부, 접근 권한)
        consultationValidator.validateProjectAccess(projectKey, userKey);

        // 대화 내역 조회
        List<ConsultationMessage> messages = consultationMessageRepository
                .findByProjectKeyOrderByCreatedAtAsc(projectKey);

        return ConsultationConverter.toHistoryResponse(projectKey, messages);
    }

    private ConsultationAiRequest buildAiRequest(
            SendConsultationRequest request,
            List<ConsultationMessage> history
    ) {
        // 대화 히스토리를 AI 형식으로 변환
        List<ConsultationAiRequest.ConversationMessage> conversationHistory =
                ConsultationConverter.toConversationHistory(history);

        return ConsultationAiRequest.builder()
                .projectKey(request.projectKey())
                .message(request.message())
                .schemaData(request.schemaData())
                .conversationHistory(conversationHistory)
                .build();
    }
}
