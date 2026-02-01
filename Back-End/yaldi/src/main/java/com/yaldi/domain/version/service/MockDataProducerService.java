package com.yaldi.domain.version.service;

import com.yaldi.domain.version.dto.kafka.MockDataCreateMessage;
import com.yaldi.infra.kafka.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock 데이터 생성 Kafka Producer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataProducerService {

    private final KafkaProducerService kafkaProducerService;

    private static final String MOCK_DATA_TOPIC = "yaldi.mockdata.create";

    //Mock 데이터 생성 메시지를 Kafka로 발행
    public void publishMockDataCreateRequest(MockDataCreateMessage message) {
        log.info("Mock 데이터 생성 요청 발행 - JobId: {}, VersionKey: {}, RowCount: {}",
                message.jobId(), message.versionKey(), message.rowCount());

        kafkaProducerService.sendMessage(MOCK_DATA_TOPIC, message.jobId(), message);
    }
}
