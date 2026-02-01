package com.yaldi.domain.erd.service;

import com.yaldi.domain.erd.dto.redis.LockInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * ErdLockService 단위 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class ErdLockServiceTest {

    @Autowired
    private ErdLockService erdLockService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // 테스트 전 Redis 정리
        redisTemplate.keys("erd:lock:table:*").forEach(redisTemplate::delete);
    }

    @Test
    @DisplayName("테이블 락 획득 성공")
    void lockTable_Success() {
        // given
        Long tableKey = 1L;
        String userEmail = "test@example.com";
        String userName = "테스터";

        // when
        boolean result = erdLockService.lockTable(tableKey, userEmail, userName);

        // then
        assertThat(result).isTrue();
        assertThat(erdLockService.isLocked(tableKey)).isTrue();
        assertThat(erdLockService.isLockedByUser(tableKey, userEmail)).isTrue();
    }

    @Test
    @DisplayName("이미 락이 있으면 실패")
    void lockTable_AlreadyLocked() {
        // given
        Long tableKey = 1L;
        erdLockService.lockTable(tableKey, "user1@example.com", "User1");

        // when
        boolean result = erdLockService.lockTable(tableKey, "user2@example.com", "User2");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("같은 사용자가 다시 락 획득하면 성공")
    void lockTable_SameUser() {
        // given
        Long tableKey = 1L;
        String userEmail = "test@example.com";
        erdLockService.lockTable(tableKey, userEmail, "Tester");

        // when
        boolean result = erdLockService.lockTable(tableKey, userEmail, "Tester");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("락 해제 성공")
    void unlockTable_Success() {
        // given
        Long tableKey = 1L;
        String userEmail = "test@example.com";
        erdLockService.lockTable(tableKey, userEmail, "Tester");

        // when
        erdLockService.unlockTable(tableKey, userEmail);

        // then
        assertThat(erdLockService.isLocked(tableKey)).isFalse();
    }

    @Test
    @DisplayName("다른 사용자는 락 해제 불가")
    void unlockTable_DifferentUser() {
        // given
        Long tableKey = 1L;
        erdLockService.lockTable(tableKey, "user1@example.com", "User1");

        // when
        erdLockService.unlockTable(tableKey, "user2@example.com");

        // then
        assertThat(erdLockService.isLocked(tableKey)).isTrue(); // 여전히 락이 유지됨
    }

    @Test
    @DisplayName("사용자별 모든 락 해제")
    void releaseAllLocksByUser() {
        // given
        String userEmail = "test@example.com";
        erdLockService.lockTable(1L, userEmail, "Tester");
        erdLockService.lockTable(2L, userEmail, "Tester");
        erdLockService.lockTable(3L, "other@example.com", "Other");

        // when
        erdLockService.releaseAllLocksByUser(userEmail);

        // then
        assertThat(erdLockService.isLocked(1L)).isFalse();
        assertThat(erdLockService.isLocked(2L)).isFalse();
        assertThat(erdLockService.isLocked(3L)).isTrue(); // 다른 사용자 락은 유지
    }

    @Test
    @DisplayName("Lock 정보 조회")
    void getLockInfo() {
        // given
        Long tableKey = 1L;
        String userEmail = "test@example.com";
        String userName = "테스터";
        erdLockService.lockTable(tableKey, userEmail, userName);

        // when
        LockInfo lockInfo = erdLockService.getLockInfo(tableKey);

        // then
        assertThat(lockInfo).isNotNull();
        assertThat(lockInfo.getUserEmail()).isEqualTo(userEmail);
        assertThat(lockInfo.getUserName()).isEqualTo(userName);
    }
}
