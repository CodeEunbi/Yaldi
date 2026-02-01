package com.yaldi.domain.comment.controller;

import com.yaldi.domain.comment.dto.request.CreateReplyRequest;
import com.yaldi.domain.comment.service.ReplyService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reply", description = "대댓글 API")
@RestController
@RequestMapping("/api/v1/replies")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    @Operation(summary = "댓글에 대댓글 생성")
    @PostMapping
    public ApiResponse<?> createReply(
            @RequestBody CreateReplyRequest request
    ) {
        return ApiResponse.onSuccess(
                replyService.createReply(
                        SecurityUtil.getCurrentUserKey(),
                        request.teamKey(),
                        request.commentKey(),
                        request.content()
                )
        );
    }

    @Operation(summary = "특정 댓글의 대댓글 목록 조회")
    @GetMapping("/comment/{commentKey}")
    public ApiResponse<?> getRepliesByComment(
            @PathVariable Long commentKey
    ) {
        return ApiResponse.onSuccess(replyService.getRepliesByComment(commentKey));
    }

    @Operation(summary = "대댓글 삭제")
    @DeleteMapping("/delete/{replyKey}")
    public ApiResponse<?> deleteReply(
            @PathVariable Long replyKey
    ) {
        replyService.deleteReply(SecurityUtil.getCurrentUserKey(), replyKey);
        return ApiResponse.OK;
    }
}
