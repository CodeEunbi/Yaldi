package com.yaldi.infra.kafka.service;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * Kafka 메시지 발행 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 메시지를 특정 토픽으로 발행
     *
     * @param topic 토픽 이름
     * @param key 메시지 키 (파티션 결정에 사용)
     * @param event 이벤트 데이터
     */
    public <T> void sendMessage(String topic, String key, T event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("메시지 전송 성공 - Topic: {}, Key: {}, Partition: {}, Offset: {}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("메시지 전송 실패 - Topic: {}, Key: {}, Error: {}",
                        topic, key, ex.getMessage());
            }
        });
    }

    /**
     * 키 없이 메시지 발행 (라운드 로빈 파티션 배정)
     */
    public <T> void sendMessage(String topic,  T event) {
        sendMessage(topic, null, event);
    }
}
