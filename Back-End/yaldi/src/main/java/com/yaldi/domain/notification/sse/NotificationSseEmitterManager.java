package com.yaldi.domain.notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationSseEmitterManager {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 10; // TIMEOUT 10분
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Integer userKey) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userKey, emitter);

        emitter.onCompletion(() -> emitters.remove(userKey));
        emitter.onTimeout(() -> emitters.remove(userKey));
        emitter.onError(e -> {
            log.warn("SSE connection error for user {}: {}", userKey, e.getMessage());
            emitters.remove(userKey);
        });

        log.info("SSE connected for user {}", userKey);
        return emitter;
    }

    public void sendToUser(Integer userKey, Object data) {
        SseEmitter emitter = emitters.get(userKey);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(data));
        } catch (IOException e) {
            emitters.remove(userKey);
        }
    }
}
