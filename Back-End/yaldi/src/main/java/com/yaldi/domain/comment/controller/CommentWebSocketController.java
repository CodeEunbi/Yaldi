package com.yaldi.domain.comment.controller;

import com.yaldi.domain.comment.dto.event.*;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CommentWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * principal 없으면 테스트 모드 → payload.userKey / payload.userName 그대로 사용
     */
    private User resolveUser(Integer userKey, Principal principal) {

        if (principal != null) {
            String email = principal.getName();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        }

        // 테스트 모드인 경우 userKey 기반으로 조회
        if (userKey != null) {
            return userRepository.findById(userKey)
                    .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        }

        throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
    }

    private void broadcast(Long projectKey, Integer userKey, WebSocketEvent event) {
        ErdBroadcastEvent collabEvent = ErdBroadcastEvent.builder()
                .projectKey(projectKey)
                .userKey(userKey)
                .event(event)
                .build();

        messagingTemplate.convertAndSend("/topic/project/" + projectKey, collabEvent);
    }


    @MessageMapping("/comment/create")
    public void createComment(@Payload CommentCreatedEvent event, Principal principal) {

        User user = resolveUser(event.getUserKey(), principal);

        // 실제 User 정보 덮어쓰기
        event.setUserKey(user.getUserKey());
        event.setUserName(user.getNickname());

        log.info("댓글 생성 WebSocket 브로드캐스트: {}", event);

        broadcast(event.getProjectKey(), user.getUserKey(), event);
    }


    @MessageMapping("/comment/delete")
    public void deleteComment(@Payload CommentDeletedEvent event, Principal principal) {

        log.info("댓글 삭제 WebSocket 브로드캐스트: {}", event);

        // principal이 없을 수 있으므로 userKey는 null
        broadcast(event.getProjectKey(), null, event);
    }

    @MessageMapping("/comment/resolve")
    public void resolveComment(@Payload CommentResolvedEvent event, Principal principal) {

        log.info("댓글 해결 변경 WebSocket 브로드캐스트: {}", event);

        broadcast(event.getProjectKey(), null, event);
    }

    @MessageMapping("/reply/create")
    public void createReply(@Payload ReplyCreatedEvent event, Principal principal) {

        User user = resolveUser(event.getUserKey(), principal);

        event.setUserKey(user.getUserKey());
        event.setUserName(user.getNickname());

        log.info("대댓글 생성 WebSocket 브로드캐스트: {}", event);

        broadcast(event.getProjectKey(), user.getUserKey(), event);
    }

    @MessageMapping("/reply/delete")
    public void deleteReply(@Payload ReplyDeletedEvent event, Principal principal) {

        log.info("대댓글 삭제 WebSocket 브로드캐스트: {}", event);

        broadcast(event.getProjectKey(), null, event);
    }
}
