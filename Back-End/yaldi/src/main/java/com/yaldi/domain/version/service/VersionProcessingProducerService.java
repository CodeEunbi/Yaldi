package com.yaldi.domain.version.service;

import com.yaldi.domain.version.dto.kafka.VersionProcessingMessage;
import com.yaldi.infra.kafka.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 버전 검증 Kafka Producer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionProcessingProducerService {

    private final KafkaProducerService kafkaProducerService;

    private static final String VERSION_VERIFICATION_TOPIC = "yaldi.version.verification";

    public void publishVersionVerificationRequest(VersionProcessingMessage message) {
        log.info("버전 검증 요청 발행 - JobId: {}, VersionKey: {}, VersionName: {}", message.jobId(), message.versionKey(), message.versionName());

        kafkaProducerService.sendMessage(VERSION_VERIFICATION_TOPIC, message.jobId(), message);
    }
}
