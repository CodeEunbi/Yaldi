package com.yaldi.domain.viewer.sse;

import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class ViewerSseEmitterManager {

    /**
     * 프로젝트별 SSE 연결 관리 Map
     *
     * Key: projectKey (프로젝트 ID)
     * Value: 해당 프로젝트를 보는 뷰어들의 SseEmitter 리스트
     *
     * ConcurrentHashMap + CopyOnWriteArrayList?
     *  - 여러 스레드가 동시에 연결/해제할 수 있음
     *  - ConcurrentHashMap: 맵 전체의 동시 접근 보호
     *  - CopyOnWriteArrayList: 리스트 순회 중 추가/삭제 안전
     */
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // SSE 타임아웃: 무제한 (0L = timeout 없음)
    // 뷰어는 읽기 전용이므로 오래 연결되어 있어도 문제없음
    // 브라우저 탭을 닫으면 onCompletion 핸들러가 자동으로 연결 정리
    // 프론트엔드에서 재연결 로직 불필요
    private static final Long SSE_TIMEOUT = 0L;

    /**
     * SSE Emitter 생성 및 등록 : 뷰어가 링크를 통해 접속할 때 호출
     *
     * ViewerSseController.connect() → createEmitter() 호출
     *
     * 1. SseEmitter 객체 생성 (무제한 타임아웃)
     * 2. 프로젝트별 연결 리스트에 추가
     * 3. 생명주기 핸들러 등록 (완료/타임아웃/에러 시 cleanup)
     * 4. 초기 연결 확인 메시지 전송
     * 5. 클라이언트에 SseEmitter 반환
     *
     */
    public SseEmitter createEmitter(Long projectKey) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        //  해당 projectKey의 리스트가 없으면 새로 생성
        emitters.computeIfAbsent(projectKey, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        log.info("SSE Emitter 생성 - ProjectKey: {}, 현재 연결 수: {}",
                projectKey, emitters.get(projectKey).size());

        // 생명주기 핸들러 등록
        //  SSE 연결은 종료 시 자동으로 리스트에서 제거 (메모리 누수 방지)

        // 정상 종료 시 (브라우저 탭 닫기 등)
        emitter.onCompletion(() -> {
            removeEmitter(projectKey, emitter);
            log.info("SSE 연결 완료 - ProjectKey: {}", projectKey);
        });

        // 타임아웃 시 (무제한이므로 거의 발생 안 함)
        emitter.onTimeout(() -> {
            removeEmitter(projectKey, emitter);
            log.warn("SSE 연결 타임아웃 - ProjectKey: {}", projectKey);
        });

        // 에러 발생 시 (네트워크 끊김 등)
        emitter.onError((ex) -> {
            removeEmitter(projectKey, emitter);
            log.error("SSE 연결 에러 - ProjectKey: {}, Error: {}", projectKey, ex.getMessage());
        });

        // 초기 연결 확인 메시지 전송, 클라이언트에게 "연결 성공" 알림
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE 연결 성공"));
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패 - ProjectKey: {}", projectKey, e);
            removeEmitter(projectKey, emitter);
        }

        return emitter;
    }

    /**
     * 특정 프로젝트의 모든 뷰어에게 ERD 업데이트 브로드캐스트
     *
     * Kafka에서 이벤트를 받으면 호출됨 됨
     * ErdBroadcastKafkaConsumerListener.consume() → sendToViewers() 호출
     *
     * 1. 해당 프로젝트의 뷰어 리스트 조회
     * 2. 뷰어가 없으면 early return (브로드캐스트할 대상 없음)
     * 3. 모든 뷰어에게 동일한 이벤트 전송
     * 4. 전송 실패한 연결은 자동 제거
     *
     */
    public void sendToViewers(Long projectKey, ErdBroadcastEvent event) {
        // 1. 해당 프로젝트의 뷰어 리스트 조회
        List<SseEmitter> projectEmitters = emitters.get(projectKey);

        // 2. 뷰어가 없으면 종료 : 아무도 보고 있지 않음
        if (projectEmitters == null || projectEmitters.isEmpty()) {
            log.debug("SSE 전송 대상 없음 - ProjectKey: {}", projectKey);
            return;
        }

        // 3. 모든 뷰어에게 이벤트 브로드캐스트
        projectEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("erd-update")
                        .data(event));
            } catch (IOException e) {
                // 전송 실패 = 연결이 끊김 → 자동 제거
                log.error("SSE 전송 실패 - ProjectKey: {}, 연결 제거", projectKey, e);
                removeEmitter(projectKey, emitter);
            }
        });
    }

    /**
     * SSE Emitter 제거 (연결 종료 시 cleanup)
     *
     * 해당 프로젝트의 뷰어 리스트에서 emitter 제거
     * 리스트가 비면 → Map에서 프로젝트 key도 제거 (메모리 절약)
     */
    private void removeEmitter(Long projectKey, SseEmitter emitter) {
        List<SseEmitter> projectEmitters = emitters.get(projectKey);

        if (projectEmitters != null) {
            projectEmitters.remove(emitter);

            // 리스트가 비면 Map에서도 제거 : 메모리 최적화
            if (projectEmitters.isEmpty()) {
                emitters.remove(projectKey);
                log.info("프로젝트의 모든 SSE 연결 종료 - ProjectKey: {}", projectKey);
            } else { // 아직 다른 뷰어가 있음
                log.info("SSE Emitter 제거 - ProjectKey: {}, 남은 연결 수: {}", projectKey, projectEmitters.size());
            }
        }
    }

    public int getViewerCount(Long projectKey) {
        List<SseEmitter> projectEmitters = emitters.get(projectKey);
        return projectEmitters != null ? projectEmitters.size() : 0;
    }
}


/**
 *    ┌────────────────────────────────────────────────────────────────┐
 *    │ Kafka → ErdBroadcastKafkaConsumerListener                     │
 *    │           ↓                                                   │
 *    │ ViewerSseEmitterManager.sendToViewers(projectKey, event)      │
 *    │           ↓                                                   │
 *    │ 해당 프로젝트의 모든 뷰어에게 브로드캐스트                           │
 *    │  - 뷰어A: emitter.send(event)                                  │
 *    │  - 뷰어B: emitter.send(event)                                  │
 *    │  - 뷰어C: emitter.send(event)                                  │
 *    └────────────────────────────────────────────────────────────────┘
 *
 *    Map<Long, CopyOnWriteArrayList<SseEmitter>>
 *    │    │                          │
 *    │    │                          └─ Thread-safe List (동시 읽기/쓰기 안전)
 *    │    └─ projectKey (123)
 *    └─ ConcurrentHashMap (동시 접근 안전)
 *
 */
