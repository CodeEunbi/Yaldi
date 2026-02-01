package com.yaldi.domain.erd.controller;

import com.yaldi.domain.erd.dto.websocket.event.ColumnOrderEvent;
import com.yaldi.domain.erd.dto.websocket.event.CursorPosEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableLockEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableMoveEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableUnlockEvent;
import com.yaldi.domain.erd.service.ErdColumnService;
import com.yaldi.domain.erd.service.ErdLockService;
import com.yaldi.domain.erd.service.ErdTableService;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * ERD WebSocket 컨트롤러 클라이언트 → 서버 → Kafka → 다른 클라이언트들
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ErdWebSocketController {

    private final ErdTableService erdTableService;
    private final ErdColumnService erdColumnService;
    private final ErdLockService erdLockService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;


    /**
     * ERD 테이블 이동 이벤트 처리 (실시간 브로드캐스트용) 클라이언트 → /pub/erd/table/move DB 저장 없이 즉시 브로드캐스트만 수행 (드래그 중) Kafka 없이 WebSocket 직접
     * 브로드캐스트 (휘발성 데이터)
     */
    @MessageMapping("/erd/table/move")
    public void handleTableMove(@Payload TableMoveEvent event, Principal principal) {
        // 사용자 정보 조회
        if (principal == null) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        String userEmail = principal.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        Integer userKey = user != null ? user.getUserKey() : null;

        TableMoveEvent moveEvent = TableMoveEvent.builder()
                .tableKey(event.getTableKey())
                .xPosition(event.getXPosition())
                .yPosition(event.getYPosition())
                .build();

        ErdBroadcastEvent broadcastEvent = ErdBroadcastEvent.builder()
                .projectKey(getProjectKeyFromTable(event.getTableKey()))
                .userKey(userKey)
                .event(moveEvent)
                .build();

        // Kafka 없이 WebSocket으로 즉시 브로드캐스트
        Long projectKey = getProjectKeyFromTable(event.getTableKey());
        messagingTemplate.convertAndSend("/topic/project/" + projectKey, broadcastEvent);
    }

    /**
     * ERD 테이블 이동 완료 이벤트 처리 (DB 저장용) 클라이언트 → /pub/erd/table/move/end 드래그 완료 시 최종 위치를 DB에 저장
     */
    @MessageMapping("/erd/table/move/end")
    public void handleTableMoveEnd(@Payload TableMoveEvent event, Principal principal) {
        log.info("Table move end event received - RAW: {}", event);
        log.info("Table move end event parsed: tableKey={}, x={}, y={}, xType={}, yType={}",
                event.getTableKey(),
                event.getXPosition(),
                event.getYPosition(),
                event.getXPosition() != null ? event.getXPosition().getClass().getName() : "null",
                event.getYPosition() != null ? event.getYPosition().getClass().getName() : "null");

        // DB에 최종 위치 저장
        erdTableService.updatePosition(event.getTableKey(), event.getXPosition(), event.getYPosition());
    }

    /**
     * 테이블 키로부터 프로젝트 키 조회
     */
    private Long getProjectKeyFromTable(Long tableKey) {
        return erdTableService.getProjectKeyByTableKey(tableKey);
    }


    /**
     * 컬럼 순서 변경 이벤트 처리 (B 타입: WebSocket + DB 저장) 클라이언트 → /pub/erd/column/reorder
     */
    @MessageMapping("/erd/column/reorder")
    public void handleColumnReorder(@Payload ColumnOrderEvent event, Principal principal) {
        log.info("Column reorder event received: columnKey={}, order={}",
                event.getColumnKey(), event.getColumnOrder());

        // 1. DB 업데이트
        erdColumnService.updateColumnOrder(event.getColumnKey(), event.getColumnOrder());

        // 2. 사용자 정보 조회
        if (principal == null) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        String userEmail = principal.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        Integer userKey = user != null ? user.getUserKey() : null;

        // 3. Kafka로 이벤트 전송
        ColumnOrderEvent orderEvent = ColumnOrderEvent.builder()
                .columnKey(event.getColumnKey())
                .columnOrder(event.getColumnOrder())
                .build();
        Long projectKey = erdColumnService.getProjectKeyByColumnKey(event.getColumnKey());
        ErdBroadcastEvent collabEvent = ErdBroadcastEvent.builder()
                .projectKey(projectKey)
                .userKey(userKey)
                .event(orderEvent)
                .build();

        messagingTemplate.convertAndSend("/topic/project/" + projectKey, collabEvent);
    }


    /**
     * 테이블 편집 시작 (잠금) 이벤트 처리 (C 타입: WebSocket + Redis 저장) 클라이언트 → /pub/erd/table/lock
     */
    @MessageMapping("/erd/table/lock")
    public void handleTableLock(@Payload TableLockEvent event, Principal principal) {
        if (principal == null) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        String userEmail = principal.getName();

        log.info("Table lock event received: tableKey={}, userEmail={}",
                event.getTableKey(), userEmail);

        // 사용자 정보 조회
        User user = userRepository.findById(Integer.parseInt(userEmail))
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // Redis에 락 정보 저장 (TTL 30초)
        boolean lockAcquired = erdLockService.lockTable(
                event.getTableKey(),
                userEmail,
                user.getNickname()
        );

        if (!lockAcquired) {
            log.warn("Failed to acquire lock for table {}: already locked", event.getTableKey());
            // TODO: 실패 시 클라이언트에게 알림 (선택적)
            return;
        }

        // Kafka로 이벤트 전송 (다른 사용자에게 알림)
        TableLockEvent lockEvent = TableLockEvent.builder()
                .tableKey(event.getTableKey())
                .userEmail(userEmail)
                .userName(user.getNickname())
                .build();

        Long projectKey = erdTableService.getProjectKeyByTableKey(event.getTableKey());
        ErdBroadcastEvent collabEvent = ErdBroadcastEvent.builder()
                .projectKey(projectKey)
                .userKey(user.getUserKey())
                .event(lockEvent)
                .build();

        messagingTemplate.convertAndSend("/topic/project/" + projectKey, collabEvent);
    }

    /**
     * 테이블 편집 종료 (잠금 해제) 이벤트 처리 (C 타입: WebSocket + Redis 저장) 클라이언트 → /pub/erd/table/unlock
     */
    @MessageMapping("/erd/table/unlock")
    public void handleTableUnlock(@Payload TableUnlockEvent event, Principal principal) {
        if (principal == null) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        String userEmail = principal.getName();

        log.info("Table unlock event received: tableKey={}, userEmail={}",
                event.getTableKey(), userEmail);

        // 사용자 정보 조회
        User user = userRepository.findById(Integer.parseInt(userEmail))
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // Redis 락 삭제
        erdLockService.unlockTable(event.getTableKey(), userEmail);

        // Kafka로 이벤트 전송
        TableUnlockEvent unlockEvent = TableUnlockEvent.builder()
                .tableKey(event.getTableKey())
                .userEmail(userEmail)
                .build();
        Long projectKey = erdTableService.getProjectKeyByTableKey(event.getTableKey());
        ErdBroadcastEvent collabEvent = ErdBroadcastEvent.builder()
                .projectKey(projectKey)
                .userKey(user.getUserKey())
                .event(unlockEvent)
                .build();

        messagingTemplate.convertAndSend("/topic/project/" + projectKey, collabEvent);
    }

    /**
     * 커서 위치 공유 이벤트 처리 (D 타입: WebSocket Only) 클라이언트 → /pub/erd/cursor DB/Redis 저장 없이 WebSocket으로 즉시 브로드캐스트 (완전 휘발성)
     */
    @MessageMapping("/erd/cursor")
    public void handleCursorMove(@Payload CursorPosEvent event, Principal principal) {
        if (principal == null) {
            throw new GeneralException(ErrorStatus.USER_NOT_FOUND);
        }

        String userEmail = principal.getName();

        // 사용자 정보 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // DB/Redis 저장 없이 WebSocket으로 즉시 브로드캐스트 (휘발성)
        CursorPosEvent cursorEvent = CursorPosEvent.builder()
                .projectKey(event.getProjectKey())
                .userEmail(userEmail)
                .userName(user.getNickname())
                .userColor(getUserColor(userEmail)) // 사용자별 색상 (해시 기반)
                .xPosition(event.getXPosition())
                .yPosition(event.getYPosition())
                .build();

        ErdBroadcastEvent broadcastEvent = ErdBroadcastEvent.builder()
                .projectKey(event.getProjectKey())
                .userKey(user.getUserKey())
                .event(cursorEvent)
                .build();

        // Kafka 없이 WebSocket으로 즉시 브로드캐스트
        messagingTemplate.convertAndSend("/topic/project/" + event.getProjectKey(), broadcastEvent);
    }

    /**
     * 사용자 이메일 기반으로 고유 색상 생성 (간단한 해시 기반)
     */
    private String getUserColor(String userEmail) {
        // 이메일 해시값을 기반으로 색상 생성
        int hash = userEmail.hashCode();
        String[] colors = {
                "#ff6b6b", "#4ecdc4", "#45b7d1", "#f7b731", "#5f27cd",
                "#00d2d3", "#1dd1a1", "#feca57", "#ee5a6f", "#c44569"
        };
        return colors[Math.abs(hash) % colors.length];
    }

    /**
     * WebSocket 연결 해제 이벤트 리스너 Lock 자동 해제 (퇴장 이벤트는 Interceptor에서 처리)
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String userEmail = principal.getName();
            log.info("Releasing locks for disconnected user: {}", userEmail);

            // 해당 사용자가 보유한 모든 락 해제
            erdLockService.releaseAllLocksByUser(userEmail);
        }
    }
}
