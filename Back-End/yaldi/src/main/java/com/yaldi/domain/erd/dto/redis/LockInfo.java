package com.yaldi.domain.erd.dto.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Redis에 저장되는 락 정보
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LockInfo implements Serializable {
    private String userEmail;
    private String userName;
    private LocalDateTime lockedAt;

    public LockInfo(String userEmail, String userName) {
        this.userEmail = userEmail;
        this.userName = userName;
        this.lockedAt = LocalDateTime.now();
    }
}
