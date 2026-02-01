package com.yaldi.infra.kafka.listener;

import com.yaldi.domain.viewer.sse.ViewerSseEmitterManager;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErdBroadcastKafkaConsumerListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ViewerSseEmitterManager viewerSseEmitterManager;

    @KafkaListener(
            topics = "yaldi.collaboration.topic",
            groupId = "yaldi-collaboration-group",
            containerFactory = "erdBroadcastKafkaListenerContainerFactory"
    )
    public void consume(ErdBroadcastEvent event) {
        ApiResponse<ErdBroadcastEvent> response = ApiResponse.onSuccess(event);

        // WebSocket 브로드캐스트 (워크스페이스 편집자들에게)
        messagingTemplate.convertAndSend("/topic/project/" + event.getProjectKey(), response);

        // SSE 브로드캐스트 (뷰어링크 접속자들에게)
        viewerSseEmitterManager.sendToViewers(event.getProjectKey(), event);
    }
}
