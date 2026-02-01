package com.yaldi.domain.comment.dto.response;

import com.yaldi.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "워크스페이스에 메모(댓글) 생성 응답 DTO")
public record CreateCommentWithoutTableResponse(

        @Schema(description = "댓글 키", example = "123")
        Long commentKey,

        @Schema(description = "작성자 사용자 키", example = "1")
        Integer userKey,

        @Schema(description = "소속 프로젝트 키", example = "2")
        Long projectKey,

        @Schema(description = "댓글 내용", example = "이 컬럼에 제약조건 추가 필요합니다.")
        String content,

        @Schema(description = "메모 색상 HEX 코드 (6자리)", example = "FFAA33")
        String colorHex,

        @Schema(description = "X 좌표", example = "320.50")
        BigDecimal xPosition,

        @Schema(description = "Y 좌표", example = "480.25")
        BigDecimal yPosition,

        @Schema(description = "생성 시각", example = "2025-11-13T14:30:00Z")
        OffsetDateTime createdAt
) {
    public static CreateCommentWithoutTableResponse from(Comment comment) {
        return new CreateCommentWithoutTableResponse(
                comment.getCommentKey(),
                comment.getUserKey(),
                comment.getProjectKey(),
                comment.getContent(),
                comment.getColorHex(),
                comment.getXPosition(),
                comment.getYPosition(),
                comment.getCreatedAt());
    }
}