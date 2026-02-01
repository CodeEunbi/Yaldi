package com.yaldi.domain.comment.dto.response;

import com.yaldi.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "댓글 목록 조회 응답 DTO")
public record CommentResponse(

        @Schema(description = "댓글 키", example = "101")
        Long commentKey,

        @Schema(description = "작성자 키", example = "1")
        Integer userKey,

        @Schema(description = "대상 테이블 키", example = "1001")
        Long tableKey,

        @Schema(description = "댓글 내용", example = "이 부분 제약조건 추가 고려해주세요.")
        String content,

        @Schema(description = "색상 HEX 코드", example = "FFAA33")
        String colorHex,

        @Schema(description = "X 좌표", example = "320.5")
        BigDecimal xPosition,

        @Schema(description = "Y 좌표", example = "480.25")
        BigDecimal yPosition,

        @Schema(description = "해결 여부", example = "false")
        Boolean isResolved,

        @Schema(description = "작성 시각", example = "2025-11-13T14:30:00Z")
        OffsetDateTime createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getCommentKey(),
                comment.getUserKey(),
                comment.getTableKey(),
                comment.getContent(),
                comment.getColorHex(),
                comment.getXPosition(),
                comment.getYPosition(),
                comment.getIsResolved(),
                comment.getCreatedAt()
        );
    }
}
