package com.yaldi.infra.websocket.dto;

import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.*;

/**
 * ERD 브로드캐스트 이벤트 (WebSocket 실시간 협업용)
 * - projectKey: 프로젝트 식별자
 * - userKey: 이벤트를 발생시킨 사용자 식별자
 * - event: 실제 이벤트 데이터 (WebSocketEvent 구현체)
 * - timestamp: 이벤트 생성 시각 (순서 보장용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdBroadcastEvent {
    private Long projectKey;
    private Integer userKey;
    private WebSocketEvent event;

    @Builder.Default
    private Long timestamp = System.currentTimeMillis();
}
