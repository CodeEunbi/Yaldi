package com.yaldi.domain.health.service;

import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LockHeartbeatService {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_KEY_PREFIX = "erd:lock:table:";
    private static final String LOCK_OWNER_SUFFIX = ":owner";
    private static final String HEARTBEAT_KEY_PREFIX = "erd:lock:heartbeat:";

    /**
     * 클라이언트에서 오는 하트비트 요청 처리
     */
    public void handleHeartbeat(Long tableId, String email) {
        String lockKey = LOCK_KEY_PREFIX + tableId;
        String ownerKey = lockKey + LOCK_OWNER_SUFFIX;

        RLock lock = redissonClient.getLock(lockKey);

        // 락 존재 여부 확인
        if (!lock.isLocked()) {
            log.warn("[HEARTBEAT REJECTED first] tableId={}, sender={}, reason=no lock exists",
                    tableId, email);
            throw new GeneralException(ErrorStatus.LOCK_ABSENT);
        }

        // Redis owner 정보 가져오기 (String 기반)
        Object rawOwner = redisTemplate.opsForValue().get(ownerKey);
        String ownerEmail = rawOwner != null ? rawOwner.toString() : null;

        // 소유자 일치 여부 확인
        if (ownerEmail == null || !ownerEmail.equals(email)) {
            log.warn("[HEARTBEAT REJECTED second] tableId={}, sender={}, actualOwner={}",
                    tableId, email, ownerEmail);
            throw new GeneralException(ErrorStatus.UNMATCH_WITH_LOCK_OWNER);
        }

        // 하트비트 갱신 승인
        pubHeartbeat(tableId, ownerKey, email);
    }

    /**
     * 하트비트 갱신
     */
    public void pubHeartbeat(Long tableId, String ownerKey, String email) {
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + tableId;

        redisTemplate.opsForValue().set(ownerKey, email, 30, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(heartbeatKey, email, 10, TimeUnit.SECONDS);

        log.debug("[HEARTBEAT UPDATED] tableId={}, owner={}", tableId, email);
    }
}
