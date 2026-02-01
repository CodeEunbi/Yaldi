package com.yaldi.domain.comment.dto.response;

import com.yaldi.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "테이블에 메모(댓글) 생성 응답 DTO")
public record CreateCommentWithTableResponse(

        @Schema(description = "댓글 키", example = "123")
        Long commentKey,

        @Schema(description = "대상 테이블 키", example = "1001")
        Long tableKey,

        @Schema(description = "소속 프로젝트 키", example = "2")
        Long projectKey,

        @Schema(description = "작성자 사용자 키", example = "1")
        Integer userKey,

        @Schema(description = "댓글 내용", example = "이 컬럼에 제약조건 추가 필요합니다.")
        String content,

        @Schema(description = "메모 색상 HEX 코드 (6자리)", example = "FFAA33")
        String colorHex,

        @Schema(description = "생성 시각", example = "2025-11-13T14:30:00Z")
        OffsetDateTime createdAt
) {
    public static CreateCommentWithTableResponse from(Comment comment) {
        return new CreateCommentWithTableResponse(
                comment.getCommentKey(),
                comment.getTableKey(),
                comment.getProjectKey(),
                comment.getUserKey(),
                comment.getContent(),
                comment.getColorHex(),
                comment.getCreatedAt());
    }
}