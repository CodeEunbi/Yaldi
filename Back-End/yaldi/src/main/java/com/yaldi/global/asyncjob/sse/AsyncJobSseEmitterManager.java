package com.yaldi.global.asyncjob.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AsyncJobSseEmitterManager {

    private static final Long TIMEOUT = 1000L * 60 * 10; // 10분
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String jobId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(jobId, emitter);

        emitter.onCompletion(() -> remove(jobId));
        emitter.onTimeout(() -> remove(jobId));
        emitter.onError(e -> {
            log.warn("SSE error for job {}: {}", jobId, e.getMessage());
            remove(jobId);
        });

        log.info("SSE connected for jobId {}", jobId);
        return emitter;
    }

    public void send(String jobId, Object data) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("import-validation")
                    .data(data));

            emitter.complete();  // 스트림 종료!!
        } catch (IOException e) {
            emitter.completeWithError(e);
            remove(jobId);
        }
    }

    private void remove(String jobId) {
        emitters.remove(jobId);
    }
}
