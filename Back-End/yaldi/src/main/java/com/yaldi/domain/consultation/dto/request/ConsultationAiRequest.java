package com.yaldi.domain.consultation.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

//AI 서버로 보낼 상담 요청 DTO
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationAiRequest {

    @JsonProperty("project_key")
    private Long projectKey;

    @JsonProperty("message")
    private String message;

    @JsonProperty("schema_data")
    private Map<String, Object> schemaData;

    @JsonProperty("conversation_history")
    private List<ConversationMessage> conversationHistory;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationMessage {
        @JsonProperty("role")
        private String role; // "user" or "assistant"

        @JsonProperty("content")
        private String content;
    }
}
