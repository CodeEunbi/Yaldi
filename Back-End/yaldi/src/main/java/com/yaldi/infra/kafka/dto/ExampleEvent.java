package com.yaldi.infra.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Kafka 메시지 예제 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExampleEvent {
    private String eventId;
    private String eventType;
    private String message;
    private LocalDateTime timestamp;
}
