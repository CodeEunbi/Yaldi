package com.yaldi.infra.kafka.controller;

import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.kafka.dto.ExampleEvent;
import com.yaldi.infra.kafka.service.KafkaProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka 테스트용 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
@Tag(name = "* Kafka Test", description = "Kafka 메시지 발행 테스트 API")
public class KafkaTestController {

    private final KafkaProducerService kafkaProducerService;

    @Operation(summary = "테스트 메시지 발행", description = "Kafka 토픽에 테스트 메시지를 발행합니다")
    @PostMapping("/send")
    public ApiResponse<Map<String, String>> sendMessage(@RequestParam String message) {
        ExampleEvent event = ExampleEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("TEST_EVENT")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        kafkaProducerService.sendMessage("yaldi.example.topic", event.getEventId(), event);

        log.info("메시지 발행 요청 완료 - EventId: {}", event.getEventId());

        return ApiResponse.onSuccess(Map.of(
                "eventId", event.getEventId(),
                "message", "메시지가 발행되었습니다"
        ));
    }

    @Operation(summary = "여러 메시지 발행", description = "지정한 개수만큼 메시지를 발행합니다")
    @PostMapping("/send-multiple")
    public ApiResponse<Map<String, Object>> sendMultipleMessages(@RequestParam(defaultValue = "10") int count) {
        for (int i = 0; i < count; i++) {
            ExampleEvent event = ExampleEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("BATCH_EVENT")
                    .message("Batch message #" + (i + 1))
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaProducerService.sendMessage("yaldi.example.topic", event);
        }

        log.info("{}개의 메시지 발행 완료", count);

        return ApiResponse.onSuccess(Map.of(
                "count", count,
                "message", count + "개의 메시지가 발행되었습니다"
        ));
    }
}
