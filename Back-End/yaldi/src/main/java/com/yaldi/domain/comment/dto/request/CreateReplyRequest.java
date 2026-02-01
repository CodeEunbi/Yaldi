package com.yaldi.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대댓글 생성 요청 DTO")
public record CreateReplyRequest(

        @Schema(description = "댓글 키", example = "10")
        Long commentKey,

        @Schema(description = "팀 키", example = "2")
        Integer teamKey,

        @Schema(description = "대댓글 내용", example = "좋은 피드백이네요!")
        String content
) {}
