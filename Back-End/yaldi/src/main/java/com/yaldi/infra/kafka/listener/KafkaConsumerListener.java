package com.yaldi.infra.kafka.listener;

import com.yaldi.infra.kafka.dto.ExampleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka 메시지 수신 리스너
 */
@Slf4j
@Component
public class KafkaConsumerListener {

    /**
     * Example 토픽 메시지 수신
     *
     * @KafkaListener 어노테이션으로 토픽을 구독
     * - topics: 구독할 토픽 이름
     * - groupId: 컨슈머 그룹 ID
     * - containerFactory: 사용할 리스너 컨테이너 팩토리 (설정에서 정의)
     */
    @KafkaListener(
            topics = "yaldi.example.topic",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeExampleEvent(
            @Payload ExampleEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("메시지 수신 - Partition: {}, Offset: {}, EventId: {}, Message: {}",
                    partition, offset, event.getEventId(), event.getMessage());

            // 여기서 비즈니스 로직 처리
            processEvent(event);

            // 수동 커밋 (application.yml에서 enable-auto-commit: false 설정)
            acknowledgment.acknowledge();

            log.info("메시지 처리 완료 - EventId: {}", event.getEventId());

        } catch (Exception e) {
            log.error("메시지 처리 실패 - Partition: {}, Offset: {}, Error: {}",
                    partition, offset, e.getMessage(), e);
            // 에러 처리 로직 (재시도, DLQ 전송 등)
        }
    }

    /**
     * 이벤트 처리 비즈니스 로직
     */
    private void processEvent(ExampleEvent event) {
        // 실제 비즈니스 로직 구현
        log.info("이벤트 처리 중: {}", event);
    }
}
