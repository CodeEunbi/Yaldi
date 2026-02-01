package com.yaldi.domain.comment.controller;

import com.yaldi.domain.comment.dto.event.CommentCreatedEvent;
import com.yaldi.domain.comment.dto.event.CommentDeletedEvent;
import com.yaldi.domain.comment.dto.event.CommentResolvedEvent;
import com.yaldi.domain.comment.dto.request.CreateCommentWithTableRequest;
import com.yaldi.domain.comment.dto.request.CreateCommentWithoutTableRequest;
import com.yaldi.domain.comment.dto.response.CreateCommentWithTableResponse;
import com.yaldi.domain.comment.dto.response.CreateCommentWithoutTableResponse;
import com.yaldi.domain.comment.entity.Comment;
import com.yaldi.domain.comment.service.CommentService;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/table")
    public ApiResponse<?> createCommentWithTable(
            @RequestBody CreateCommentWithTableRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();

        Comment saved = commentService.createCommentWithTable(
                userKey,
                request.teamKey(),
                request.projectKey(),
                request.tableKey(),
                request.content(),
                request.colorHex()
        );

        publishCreatedEvent(saved);

        return ApiResponse.onSuccess(CreateCommentWithTableResponse.from(saved));
    }

    @PostMapping("/workspace")
    public ApiResponse<?> createCommentWithoutTable(
            @RequestBody CreateCommentWithoutTableRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();

        Comment saved = commentService.createCommentWithoutTable(
                userKey, request.teamKey(), request.projectKey(),
                request.content(), request.colorHex(),
                request.xPosition(), request.yPosition()
        );

        publishCreatedEvent(saved);

        return ApiResponse.onSuccess(CreateCommentWithoutTableResponse.from(saved));
    }

    @DeleteMapping("/delete/{commentKey}")
    public ApiResponse<?> deleteComment(@PathVariable Long commentKey) {
        Comment deleted = commentService.deleteComment(SecurityUtil.getCurrentUserKey(), commentKey);

        publishDeletedEvent(deleted);

        return ApiResponse.OK;
    }

    @PatchMapping("/{commentKey}/resolve")
    public ApiResponse<?> resolveComment(@PathVariable Long commentKey) {
        Comment resolved = commentService.resolveComment(commentKey);

        publishResolvedEvent(resolved, true);

        return ApiResponse.OK;
    }

    @PatchMapping("/{commentKey}/unresolve")
    public ApiResponse<?> unResolveComment(@PathVariable Long commentKey) {
        Comment unresolved = commentService.unResolveComment(commentKey);

        publishResolvedEvent(unresolved, false);

        return ApiResponse.OK;
    }


    /** WebSocket 이벤트 (생성) */
    private void publishCreatedEvent(Comment comment) {
        User user = userRepository.findById(comment.getUserKey()).orElse(null);

        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .commentKey(comment.getCommentKey())
                .projectKey(comment.getProjectKey())
                .tableKey(comment.getTableKey())
                .userKey(comment.getUserKey())
                .userName(user != null ? user.getNickname() : null)
                .content(comment.getContent())
                .colorHex(comment.getColorHex())
                .xPosition(comment.getXPosition())
                .yPosition(comment.getYPosition())
                .isResolved(comment.getIsResolved())
                .build();

        send(comment.getProjectKey(), comment.getUserKey(), event);
    }

    /** WebSocket 이벤트 (삭제) */
    private void publishDeletedEvent(Comment comment) {
        CommentDeletedEvent event = CommentDeletedEvent.builder()
                .commentKey(comment.getCommentKey())
                .projectKey(comment.getProjectKey())
                .build();

        send(comment.getProjectKey(), comment.getUserKey(), event);
    }

    /** WebSocket 이벤트 (해결/취소) */
    private void publishResolvedEvent(Comment comment, boolean resolved) {
        CommentResolvedEvent event = CommentResolvedEvent.builder()
                .commentKey(comment.getCommentKey())
                .projectKey(comment.getProjectKey())
                .resolved(resolved)
                .build();

        send(comment.getProjectKey(), comment.getUserKey(), event);
    }

    /** 공통 send() */
    private void send(Long projectKey, Integer userKey, WebSocketEvent event) {
        messagingTemplate.convertAndSend("/topic/project/" + projectKey,
                ErdBroadcastEvent.builder()
                        .projectKey(projectKey)
                        .userKey(userKey)
                        .event(event)
                        .build());
    }
}
