package com.yaldi.infra.kafka.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaldi.global.asyncjob.dto.ValidateImportRequest;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import com.yaldi.global.asyncjob.sse.AsyncJobSseEmitterManager;
import com.yaldi.global.asyncjob.service.AsyncJobService;
import com.yaldi.infra.kafka.dto.ImportSqlEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportSqlConsumer {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private final AsyncJobService asyncJobService;
    private final AsyncJobSseEmitterManager sseManager;

    private static final String AI_URL = "http://localhost:8000/api/v1/erd/validate-import";

    @KafkaListener(
            topics = "async-job-topic",
            groupId = "yaldi-async-group",
            containerFactory = "importSqlKafkaListenerContainerFactory"
    )
    public void consume(ImportSqlEvent event) throws InterruptedException {

        log.info("[Kafka 수신] JobId={}, ProjectKey={}", event.getJobId(), event.getProjectKey());
        asyncJobService.updateStatus(event.getJobId(), AsyncJobStatus.PROCESSING);

        // AI 서버에 보낼 DTO 구성
        ValidateImportRequest request = new ValidateImportRequest(
                event.getJobId(),
                String.valueOf(event.getUserKey()),
                String.valueOf(event.getProjectKey()),
                event.getSqlContent(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        );

        try {
            String aiResponse = webClient.post()
                    .uri(AI_URL)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("[AI 검증 완료] JobId={} / 응답={}", event.getJobId(), aiResponse);

            // SSE PUSH!
            Thread.sleep(300);
            sseManager.send(event.getJobId(), aiResponse);

            // JOB 완료 처리
            asyncJobService.updateStatus(event.getJobId(), AsyncJobStatus.COMPLETED);

        } catch (Exception e) {

            log.error("[AI 검증 실패] JobId={}, 이유={}", event.getJobId(), e.getMessage());
            asyncJobService.failJob(event.getJobId(), e.getMessage());

            // error SSE push 가능
            Thread.sleep(300);
            sseManager.send(event.getJobId(), "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
