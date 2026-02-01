package com.yaldi.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "테이블에 메모(댓글) 생성 요청 DTO")
public record CreateCommentWithTableRequest(
        @Schema(description = "소속 팀 키", example = "2")
        Integer teamKey,

        @Schema(description = "대상 테이블 키", example = "1001")
        Long tableKey,

        @Schema(description = "소속 프로젝트 키", example = "2")
        Long projectKey,

        @Schema(description = "댓글 내용", example = "이 부분은 인덱스 추가 고려 필요합니다.")
        String content,

        @Schema(description = "메모 색상 HEX 코드 (6자리)", example = "FFAA33")
        String colorHex
) {
}