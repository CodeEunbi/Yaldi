package com.yaldi.domain.consultation.dto.response;

import com.yaldi.domain.consultation.entity.ConsultationMessageRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "상담 메시지")
public record ConsultationMessageResponse(
    @Schema(description = "메시지 ID", example = "1")
    Long messageKey,

    @Schema(description = "역할", example = "USER")
    ConsultationMessageRole role,

    @Schema(description = "메시지 내용", example = "User 테이블 PK를 어떻게 설정하면 좋을까요?")
    String message,

    @Schema(description = "AI 응답 시: 적용 가능한 스키마 수정사항")
    List<Map<String, Object>> schemaModifications,

    @Schema(description = "AI 응답 시: 확신도", example = "0.88")
    Float confidence,

    @Schema(description = "AI 응답 시: 사용된 Agent 목록", example = "[\"NormalizationExpert\", \"IndexStrategyExpert\"]")
    List<String> agentsUsed,

    @Schema(description = "AI 응답 시: 경고 사항", example = "[\"기존 쿼리 수정 필요\"]")
    List<String> warnings,

    @Schema(description = "생성 시각")
    OffsetDateTime createdAt
) {
}
