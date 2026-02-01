package com.yaldi.global.util;

import com.yaldi.domain.health.service.LockHeartbeatService;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LockUtil {

    private final RedissonClient redissonClient;
    private final LockHeartbeatService heartbeatService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_KEY_PREFIX = "lock:table:";
    private static final String LOCK_OWNER_SUFFIX = ":owner";

    public boolean tryLock(Long tableId, String email) {
        String lockKey = LOCK_KEY_PREFIX + tableId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(30, 0, TimeUnit.SECONDS);

            if (acquired) {
                String ownerKey = lockKey + LOCK_OWNER_SUFFIX;
                redisTemplate.opsForValue().set(ownerKey, email);
                heartbeatService.pubHeartbeat(tableId, ownerKey, email);
            }

            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(Long tableId, String email) {
        String lockKey = LOCK_KEY_PREFIX + tableId;
        String ownerKey = lockKey + LOCK_OWNER_SUFFIX;

        RLock lock = redissonClient.getLock(lockKey);
        Object currentOwner = redisTemplate.opsForValue().get(ownerKey);

        if(!lock.isLocked()) {
            throw new GeneralException(ErrorStatus.LOCK_ABSENT);
        }
        if (currentOwner == null || !currentOwner.equals(email)) {
            throw new GeneralException(ErrorStatus.UNMATCH_WITH_LOCK_OWNER);
        }
        try {
            if (lock.isLocked()) {
                lock.forceUnlock();
                redisTemplate.delete(ownerKey);
            }
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus.FAIL_TO_UNLOCK);
        }
    }

    public String getLockOwner(Long tableId) {
        String ownerKey = LOCK_KEY_PREFIX + tableId + LOCK_OWNER_SUFFIX;
        Object owner = redisTemplate.opsForValue().get(ownerKey);
        return owner != null ? owner.toString() : null;
    }

    public void forceUnlock(Long tableId, String email) {
        String lockKey = LOCK_KEY_PREFIX + tableId;
        String ownerKey = lockKey + LOCK_OWNER_SUFFIX;

        RLock lock = redissonClient.getLock(lockKey);
        lock.forceUnlock();
        redisTemplate.delete(ownerKey);
        log.info("[FORCE UNLOCKED] tableId={}, owner={}", tableId, email);
    }
}
