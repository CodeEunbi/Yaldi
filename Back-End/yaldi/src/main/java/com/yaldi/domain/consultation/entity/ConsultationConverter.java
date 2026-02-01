package com.yaldi.domain.consultation.entity;

import com.yaldi.domain.consultation.dto.request.ConsultationAiRequest;
import com.yaldi.domain.consultation.dto.response.ConsultationHistoryResponse;
import com.yaldi.domain.consultation.dto.response.ConsultationMessageResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Consultation 도메인의 Entity ↔ DTO 변환을 담당하는 Converter
 */
public class ConsultationConverter {

    private ConsultationConverter() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * ConsultationMessage Entity를 ConsultationMessageResponse DTO로 변환
     *
     * @param message 변환할 Entity
     * @return 변환된 Response DTO
     */
    public static ConsultationMessageResponse toResponse(ConsultationMessage message) {
        return new ConsultationMessageResponse(
                message.getMessageKey(),
                message.getRole(),
                message.getMessage(),
                message.getSchemaModifications(),
                message.getConfidence(),
                message.getAgentsUsed(),
                message.getWarnings(),
                message.getCreatedAt()
        );
    }

    /**
     * ConsultationMessage 리스트를 ConsultationHistoryResponse로 변환
     *
     * @param projectKey 프로젝트 키
     * @param messages   변환할 메시지 리스트
     * @return 히스토리 Response DTO
     */
    public static ConsultationHistoryResponse toHistoryResponse(Long projectKey, List<ConsultationMessage> messages) {
        List<ConsultationMessageResponse> messageResponses = messages.stream()
                .map(ConsultationConverter::toResponse)
                .collect(Collectors.toList());

        return new ConsultationHistoryResponse(
                projectKey,
                messages.size(),
                messageResponses
        );
    }

    /**
     * ConsultationMessage 리스트를 AI 요청용 ConversationMessage 리스트로 변환
     *
     * @param messages 변환할 메시지 리스트
     * @return AI 요청용 대화 히스토리
     */
    public static List<ConsultationAiRequest.ConversationMessage> toConversationHistory(
            List<ConsultationMessage> messages
    ) {
        return messages.stream()
                .map(msg -> ConsultationAiRequest.ConversationMessage.builder()
                        .role(msg.getRole() == ConsultationMessageRole.USER ? "user" : "assistant")
                        .content(msg.getMessage())
                        .build())
                .collect(Collectors.toList());
    }
}
