package com.yaldi.domain.comment.dto.response;

import com.yaldi.domain.comment.entity.Reply;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "대댓글 생성 응답 DTO")
public record CreateReplyResponse(

        @Schema(description = "대댓글 키", example = "101")
        Long replyKey,

        @Schema(description = "부모 댓글 키", example = "10")
        Long commentKey,

        @Schema(description = "작성자 키", example = "1")
        Integer userKey,

        @Schema(description = "대댓글 내용", example = "좋은 피드백이네요!")
        String content,

        @Schema(description = "생성 시각", example = "2025-11-13T14:30:00Z")
        OffsetDateTime createdAt
) {
    public static CreateReplyResponse from(Reply reply) {
        return new CreateReplyResponse(
                reply.getReplyKey(),
                reply.getCommentKey(),
                reply.getUserKey(),
                reply.getContent(),
                reply.getCreatedAt()
        );
    }
}
