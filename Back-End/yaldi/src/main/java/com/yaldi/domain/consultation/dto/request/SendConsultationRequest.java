package com.yaldi.domain.consultation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "ERD 상담 질문 전송 요청")
public record SendConsultationRequest(
    @Schema(description = "프로젝트 ID", example = "1")
    @NotNull(message = "프로젝트 ID는 필수입니다")
    @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다")
    Long projectKey,

    @Schema(description = "사용자 질문", example = "User 테이블을 정규화하면서 조회 성능도 고려해야 하나요?")
    @NotBlank(message = "질문 내용은 필수입니다")
    String message,

    @Schema(description = "현재 스키마 데이터 (프론트에서 현재 ERD 상태를 추출하여 전송)")
    @NotNull(message = "스키마 데이터는 필수입니다")
    Map<String, Object> schemaData
) {
}
