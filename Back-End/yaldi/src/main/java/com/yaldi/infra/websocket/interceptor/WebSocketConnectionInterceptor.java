package com.yaldi.infra.websocket.interceptor;

import com.yaldi.domain.erd.dto.websocket.event.MemberJoinEvent;
import com.yaldi.domain.erd.dto.websocket.event.MemberLeaveEvent;
import com.yaldi.domain.project.service.ProjectAccessValidator;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import com.yaldi.infra.websocket.service.ErdBroadcastBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Connect 시점에 프로젝트 입장 감지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionInterceptor implements ChannelInterceptor {

    private final UserRepository userRepository;
    private final ErdBroadcastBatchService erdBroadcastBatchService;
    private final ProjectAccessValidator projectAccessValidator;

    // 세션별 프로젝트 매핑
    private final Map<String, Long> sessionProjectMap = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            // CONNECT 시점 처리 - 프로젝트 입장
            if (StompCommand.CONNECT.equals(command)) {
                handleConnect(accessor);
            }
            // DISCONNECT 시점 처리 - 프로젝트 퇴장
            else if (StompCommand.DISCONNECT.equals(command)) {
                handleDisconnect(accessor);
            }
        }

        return message;
    }

    /**
     * Connect 시점: projectKey 헤더로 프로젝트 입장
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        try {
            Principal principal = accessor.getUser();
            String sessionId = accessor.getSessionId();

            // projectKey 헤더 추출
            String projectKeyStr = accessor.getFirstNativeHeader("projectKey");

            if (principal != null && sessionId != null && projectKeyStr != null) {
                String userEmail = principal.getName();
                Long projectKey = Long.parseLong(projectKeyStr);

                // 사용자 정보 조회
                User user = userRepository.findByEmail(userEmail).orElse(null);
                if (user == null) {
                    log.warn("User not found: {}", userEmail);
                    return;
                }

                // 프로젝트 접근 권한 검증
                try {
                    projectAccessValidator.validateProjectAccess(projectKey, user.getUserKey());
                } catch (Exception e) {
                    log.warn("Unauthorized WebSocket connection attempt: user={}, project={}", userEmail, projectKey);
                    return; // 권한 없으면 연결 거부 (조용히 무시)
                }

                // 세션-프로젝트 매핑 저장
                sessionProjectMap.put(sessionId, projectKey);

                log.info("WebSocket CONNECT: user={}, project={}, session={}",
                        userEmail, projectKey, sessionId);

                // 입장 이벤트 브로드캐스트
                MemberJoinEvent joinEvent = MemberJoinEvent.builder()
                        .projectKey(projectKey)
                        .userEmail(userEmail)
                        .userName(user.getNickname())
                        .userColor(getUserColor(userEmail))
                        .build();

                ErdBroadcastEvent collabEvent = ErdBroadcastEvent.builder()
                        .projectKey(projectKey)
                        .userKey(user.getUserKey())
                        .event(joinEvent)
                        .build();

                erdBroadcastBatchService.collectEvent(collabEvent);

                log.info("User {} joined project {} at CONNECT", userEmail, projectKey);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket CONNECT", e);
        }
    }

    /**
     * Disconnect 시점: 퇴장 이벤트 발행
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        try {
            Principal principal = accessor.getUser();
            String sessionId = accessor.getSessionId();

            if (principal != null && sessionId != null) {
                String userEmail = principal.getName();
                Long projectKey = sessionProjectMap.remove(sessionId);

                if (projectKey != null) {
                    log.info("WebSocket DISCONNECT: user={}, project={}, session={}",
                            userEmail, projectKey, sessionId);

                    // 사용자 정보 조회
                    User user = userRepository.findByEmail(userEmail).orElse(null);

                    // 퇴장 이벤트 브로드캐스트
                    MemberLeaveEvent leaveEvent = MemberLeaveEvent.builder()
                            .projectKey(projectKey)
                            .userEmail(userEmail)
                            .userName(user != null ? user.getNickname() : userEmail)
                            .userColor(getUserColor(userEmail))
                            .build();

                    ErdBroadcastEvent collabEvent = ErdBroadcastEvent.builder()
                            .projectKey(projectKey)
                            .userKey(user != null ? user.getUserKey() : null)
                            .event(leaveEvent)
                            .build();

                    erdBroadcastBatchService.collectEvent(collabEvent);

                    log.info("User {} left project {} at DISCONNECT", userEmail, projectKey);
                }
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket DISCONNECT", e);
        }
    }

    /**
     * 사용자 색상 생성 (이메일 해시 기반)
     */
    private String getUserColor(String userEmail) {
        int hash = userEmail.hashCode();
        String[] colors = {
            "#ff6b6b", "#4ecdc4", "#45b7d1", "#f7b731", "#5f27cd",
            "#00d2d3", "#1dd1a1", "#feca57", "#ee5a6f", "#c44569"
        };
        return colors[Math.abs(hash) % colors.length];
    }

    /**
     * 세션의 프로젝트 키 조회 (외부에서 사용 가능)
     */
    public Long getProjectKeyBySession(String sessionId) {
        return sessionProjectMap.get(sessionId);
    }
}
