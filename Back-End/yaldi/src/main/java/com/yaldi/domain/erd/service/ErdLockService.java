package com.yaldi.domain.erd.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaldi.domain.erd.dto.redis.LockInfo;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErdLockService {

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String LOCK_KEY_PREFIX = "erd:lock:table:";
    private static final String LOCK_OWNER_SUFFIX = ":owner";
    private static final String HEARTBEAT_KEY_PREFIX = "erd:lock:heartbeat:";
    private static final long LOCK_TTL_SECONDS = 30L;
    private static final long HEARTBEAT_TTL_SECONDS = 10L;

    public boolean lockTable(Long tableKey, String userEmail, String userName) {
        String lockKey = LOCK_KEY_PREFIX + tableKey;
        String ownerKey = lockKey + LOCK_OWNER_SUFFIX;

        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(0, -1, TimeUnit.SECONDS);

            if (acquired) {
                // 🔥 ownerKey에는 email만 저장
                redisTemplate.opsForValue().set(ownerKey, userEmail, -1, TimeUnit.SECONDS);

                publishHeartbeat(tableKey, userEmail);

                log.info("Table {} locked by {} ({})", tableKey, userEmail, userName);
                return true;
            } else {
                Object rawOwner = redisTemplate.opsForValue().get(ownerKey);
                String ownerEmail = rawOwner != null ? rawOwner.toString() : "unknown";

                log.warn("Table {} is already locked by {} (requested by {})",
                        tableKey, ownerEmail, userEmail);

                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to acquire lock for table {}: interrupted", tableKey);
            return false;
        }
    }

    public void unlockTable(Long tableKey, String userEmail) {
        String lockKey = LOCK_KEY_PREFIX + tableKey;
        String ownerKey = lockKey + LOCK_OWNER_SUFFIX;

        Object rawOwner = redisTemplate.opsForValue().get(ownerKey);
        String existingOwner = rawOwner != null ? rawOwner.toString() : null;

        if (existingOwner != null && existingOwner.equals(userEmail)) {

            RLock lock = redissonClient.getLock(lockKey);
            try {
                if (lock.isHeldByCurrentThread()) lock.unlock();
                else if (lock.isLocked()) lock.forceUnlock();
            } catch (Exception e) {
                log.warn("Error unlocking table {}: {}", tableKey, e.getMessage());
            }

            redisTemplate.delete(ownerKey);
            redisTemplate.delete(HEARTBEAT_KEY_PREFIX + tableKey);

            log.info("Table {} unlocked by {}", tableKey, userEmail);
        } else {
            log.warn("Cannot unlock table {}: not locked by {}", tableKey, userEmail);
        }
    }

    public LockInfo getLockInfo(Long tableKey) {
        String ownerKey = LOCK_KEY_PREFIX + tableKey + LOCK_OWNER_SUFFIX;
        Object raw = redisTemplate.opsForValue().get(ownerKey);
        String ownerEmail = raw != null ? raw.toString() : null;

        if (ownerEmail == null) return null;

        return new LockInfo(ownerEmail, null);
    }

    public boolean isLocked(Long tableKey) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tableKey);
        return lock.isLocked();
    }

    public boolean isLockedByUser(Long tableKey, String userEmail) {
        LockInfo lockInfo = getLockInfo(tableKey);
        return lockInfo != null && lockInfo.getUserEmail().equals(userEmail);
    }

    public void releaseAllLocksByUser(String userEmail) {

        var scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(LOCK_KEY_PREFIX + "*" + LOCK_OWNER_SUFFIX)
                .count(100)
                .build();

        int releasedCount = 0;

        try (var cursor = redisTemplate.scan(scanOptions)) {

            while (cursor.hasNext()) {
                String ownerKey = cursor.next();
                Object rawOwner = redisTemplate.opsForValue().get(ownerKey);
                String ownerEmail = rawOwner != null ? rawOwner.toString() : null;

                if (ownerEmail != null && ownerEmail.equals(userEmail)) {

                    String lockKeyStr = ownerKey.replace(LOCK_OWNER_SUFFIX, "");
                    String tableKeyStr = lockKeyStr.replace(LOCK_KEY_PREFIX, "");

                    try {
                        Long tableKey = Long.parseLong(tableKeyStr);

                        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tableKey);
                        if (lock.isLocked()) lock.forceUnlock();

                        redisTemplate.delete(ownerKey);
                        redisTemplate.delete(HEARTBEAT_KEY_PREFIX + tableKey);

                        releasedCount++;
                        log.info("Released lock {} for disconnected user {}", tableKey, userEmail);

                    } catch (NumberFormatException e) {
                        log.warn("Invalid table key format: {}", tableKeyStr);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error while scanning locks: {}", e.getMessage(), e);
        }

        if (releasedCount > 0) {
            log.info("Released {} locks for user {}", releasedCount, userEmail);
        }
    }

    public void publishHeartbeat(Long tableKey, String userEmail) {
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + tableKey;
        String ownerKey = LOCK_KEY_PREFIX + tableKey + LOCK_OWNER_SUFFIX;

        Object rawOwner = redisTemplate.opsForValue().get(ownerKey);
        String ownerEmail = rawOwner != null ? rawOwner.toString() : null;

        if (ownerEmail != null && ownerEmail.equals(userEmail)) {
            redisTemplate.opsForValue().set(heartbeatKey, userEmail,
                    HEARTBEAT_TTL_SECONDS, TimeUnit.SECONDS);

            log.debug("Heartbeat published for table {} by {}", tableKey, userEmail);
        } else {
            log.warn("Heartbeat rejected for table {}: not owned by {}", tableKey, userEmail);
        }
    }

    public void handleHeartbeat(Long tableKey, String userEmail) {
        publishHeartbeat(tableKey, userEmail);
    }

    public boolean isHeartbeatAlive(Long tableKey) {
        String heartbeatKey = HEARTBEAT_KEY_PREFIX + tableKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey));
    }

    public void validateTableLock(Long tableKey) {
        Integer userEmail = SecurityUtil.getCurrentUserKey();

        if (!isLocked(tableKey)) {
            throw new GeneralException(ErrorStatus.LOCK_ABSENT);
        }

        if (!isLockedByUser(tableKey, userEmail.toString())) {
            throw new GeneralException(ErrorStatus.UNMATCH_WITH_LOCK_OWNER);
        }
    }

    private LockInfo convertToLockInfo(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.convertValue(value, LockInfo.class);
        } catch (Exception e) {
            return null;
        }
    }
}
