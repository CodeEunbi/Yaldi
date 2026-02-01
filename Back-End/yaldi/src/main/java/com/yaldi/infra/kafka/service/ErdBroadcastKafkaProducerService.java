package com.yaldi.infra.kafka.service;

import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErdBroadcastKafkaProducerService {
    private final KafkaProducerService kafkaProducerService;

    private static final String ERD_BROADCAST_TOPIC = "yaldi.collaboration.topic";

    public void publish(ErdBroadcastEvent event) {
        kafkaProducerService.sendMessage(ERD_BROADCAST_TOPIC, event);
    }
}
