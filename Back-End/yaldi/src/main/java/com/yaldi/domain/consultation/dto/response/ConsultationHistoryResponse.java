package com.yaldi.domain.consultation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "상담 대화 내역 전체")
public record ConsultationHistoryResponse(
    @Schema(description = "프로젝트 ID", example = "1")
    Long projectKey,

    @Schema(description = "전체 메시지 개수", example = "15")
    int totalCount,

    @Schema(description = "메시지 목록 (시간순)")
    List<ConsultationMessageResponse> messages
) {
}
