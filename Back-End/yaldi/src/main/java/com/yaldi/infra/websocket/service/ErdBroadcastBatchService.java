package com.yaldi.infra.websocket.service;

import com.yaldi.infra.kafka.service.ErdBroadcastKafkaProducerService;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErdBroadcastBatchService {

    private final ErdBroadcastKafkaProducerService kafkaProducerService;

    private final Map<Long, Map<String, List<ErdBroadcastEvent>>> eventBuffer = new ConcurrentHashMap<>();

    // 압축 가능한 이벤트 타입 (마지막 것만 유효한 이벤트)
    private static final Set<String> COMPRESSIBLE_EVENTS = Set.of(
        "CURSOR_MOVE",      // 커서 위치 - 마지막 위치만 중요
        "TABLE_MOVE"        // 테이블 드래그 중 - 마지막 위치만 중요
    );

    public void collectEvent(ErdBroadcastEvent event) {
        eventBuffer
                .computeIfAbsent(event.getProjectKey(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(String.valueOf(event.getUserKey()), k -> new ArrayList<>())
                .add(event);
    }

    @Scheduled(fixedRate = 2000)
    public void flush() {
        eventBuffer.forEach((projectKey, senderMap) -> {
            senderMap.forEach((sender, events) -> {
                if (events.isEmpty()) return;

                // 1. 타임스탬프 순으로 정렬 (순서 보장)
                events.sort((e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()));

                // 2. 이벤트 타입별로 그룹화 (순서 유지)
                Map<String, List<ErdBroadcastEvent>> groupedByType = events.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getEvent().getType(),
                                java.util.LinkedHashMap::new,  // 순서 보장을 위해 LinkedHashMap 사용
                                Collectors.toList()
                        ));

                int originalCount = events.size();
                int compressedCount = 0;

                // 3. 타입별로 압축 처리
                for (Map.Entry<String, List<ErdBroadcastEvent>> entry : groupedByType.entrySet()) {
                    String eventType = entry.getKey();
                    List<ErdBroadcastEvent> typeEvents = entry.getValue();

                    if (COMPRESSIBLE_EVENTS.contains(eventType) && typeEvents.size() > 1) {
                        // 압축 가능한 이벤트: 타임스탬프가 가장 최근인 것만 전송
                        ErdBroadcastEvent latest = typeEvents.stream()
                                .max((e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()))
                                .orElse(typeEvents.get(typeEvents.size() - 1));
                        kafkaProducerService.publish(latest);
                        compressedCount++;

                        log.debug("Compressed {} {} events to 1 (projectKey={}, user={})",
                                typeEvents.size(), eventType, projectKey, sender);
                    } else {
                        // 압축 불가능한 이벤트: 타임스탬프 순으로 모두 전송
                        typeEvents.forEach(kafkaProducerService::publish);
                        compressedCount += typeEvents.size();
                    }
                }

                if (originalCount > compressedCount) {
                    log.info("Batch optimization: {} events → {} events ({}% reduced, projectKey={}, user={})",
                            originalCount, compressedCount,
                            (originalCount - compressedCount) * 100 / originalCount,
                            projectKey, sender);
                }

                events.clear();
            });

            // 4. 메모리 누수 방지: 빈 사용자 맵 제거
            senderMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        });

        // 5. 메모리 누수 방지: 빈 프로젝트 맵 제거
        eventBuffer.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
