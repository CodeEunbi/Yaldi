package com.yaldi.infra.redis.schedule;

import com.yaldi.global.util.LockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LockCleanupService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final LockUtil lockUtil;

    private static final String LOCK_KEY_PREFIX = "erd:lock:table:";
    private static final String LOCK_OWNER_SUFFIX = ":owner";
    private static final String HEARTBEAT_KEY_PREFIX = "erd:lock:heartbeat:";

    @Scheduled(fixedRate = 30000)
    public void releaseStaleLocks() {
        int cleanedCount = 0;

        var scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(LOCK_KEY_PREFIX + "*" + LOCK_OWNER_SUFFIX)
                .count(100)
                .build();

        try (var cursor = redisTemplate.scan(scanOptions)) {

            while (cursor.hasNext()) {
                String ownerKey = cursor.next();

                // tableId 추출
                String lockKeyStr = ownerKey.replace(LOCK_OWNER_SUFFIX, "");
                String tableIdStr = lockKeyStr.replace(LOCK_KEY_PREFIX, "");

                try {
                    Long tableId = Long.parseLong(tableIdStr);
                    String heartbeatKey = HEARTBEAT_KEY_PREFIX + tableId;

                    // 하트비트 살아있으면 skip
                    if (Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey))) {
                        continue;
                    }

                    // owner 조회 (문자열)
                    Object raw = redisTemplate.opsForValue().get(ownerKey);
                    String ownerEmail = raw != null ? raw.toString() : null;

                    if (ownerEmail != null) {
                        lockUtil.forceUnlock(tableId, ownerEmail);

                        cleanedCount++;
                        log.info("Cleaned stale lock for table {} (owner: {})", tableId, ownerEmail);
                    }

                } catch (NumberFormatException e) {
                    log.warn("Invalid table ID format in key: {}", ownerKey);
                }
            }

        } catch (Exception e) {
            log.error("Error during lock cleanup: {}", e.getMessage(), e);
        }

        if (cleanedCount > 0) {
            log.info("Lock cleanup completed: {} stale locks released", cleanedCount);
        }
    }
}
