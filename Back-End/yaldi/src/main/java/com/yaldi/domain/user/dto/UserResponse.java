package com.yaldi.domain.user.dto;

import com.yaldi.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 사용자 정보 응답 DTO
 */
@Getter
@Builder
public class UserResponse {

    private Integer userKey;
    private String email;
    private String nickname;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * User 엔티티를 UserResponse DTO로 변환
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userKey(user.getUserKey())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
