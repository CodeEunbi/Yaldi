package com.yaldi.domain.comment.dto.response;

import com.yaldi.domain.comment.entity.Reply;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "대댓글 조회 응답 DTO")
public record ReplyResponse(
        @Schema(description = "대댓글 키", example = "201")
        Long replyKey,

        @Schema(description = "댓글 키", example = "101")
        Long commentKey,

        @Schema(description = "작성자 키", example = "3")
        Integer userKey,

        @Schema(description = "대댓글 내용", example = "좋은 의견이에요!")
        String content,

        @Schema(description = "작성 시각", example = "2025-11-13T14:45:00Z")
        OffsetDateTime createdAt
) {
    public static ReplyResponse from(Reply reply) {
        return new ReplyResponse(
                reply.getReplyKey(),
                reply.getCommentKey(),
                reply.getUserKey(),
                reply.getContent(),
                reply.getCreatedAt()
        );
    }
}
