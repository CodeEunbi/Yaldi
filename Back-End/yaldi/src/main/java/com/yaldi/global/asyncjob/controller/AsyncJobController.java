package com.yaldi.global.asyncjob.controller;

import com.yaldi.global.asyncjob.sse.AsyncJobSseEmitterManager;
import com.yaldi.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/async-jobs")
public class AsyncJobController {

    private final AsyncJobSseEmitterManager asyncJobSseEmitterManager;

    /**
     * 비동기 작업(jobId) 결과를 실시간으로 수신하기 위한 SSE 구독 API
     *
     * 클라이언트는 다음과 같이 구독한다:
     *   const evtSource = new EventSource(`/api/v1/async-jobs/{jobId}/subscribe`);
     *   evtSource.onmessage = (event) => console.log(JSON.parse(event.data));
     *
     * @param jobId AsyncJob 식별자
     * @return SseEmitter 스트림
     */
    @GetMapping("/{jobId}/subscribe")
    public SseEmitter subscribe(@PathVariable String jobId) {
        log.info("[SSE 구독 요청] jobId={}", jobId);

        // 새로운 SSE 스트림 생성
        SseEmitter emitter = asyncJobSseEmitterManager.createEmitter(jobId);

        // 연결 직후 클라이언트가 "연결됨"을 알 수 있게 ping 전송 (선택)
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(ApiResponse.onSuccess("SSE connected for jobId=" + jobId)));
        } catch (Exception e) {
            log.error("초기 SSE 전송 오류 jobId={}, error={}", jobId, e.getMessage());
        }

        return emitter;
    }
}
