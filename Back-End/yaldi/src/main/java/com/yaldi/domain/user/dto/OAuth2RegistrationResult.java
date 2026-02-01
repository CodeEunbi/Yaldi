package com.yaldi.domain.user.dto;

import com.yaldi.domain.user.entity.AuthType;
import com.yaldi.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OAuth2 사용자 등록 결과
 *
 * <p>OAuth2 로그인 시 사용자 정보와 인증 유형을 함께 반환합니다.</p>
 *
 * @author Yaldi Team
 * @version 1.0
 */
@Getter
@AllArgsConstructor
public class OAuth2RegistrationResult {
    /**
     * 사용자 엔티티
     */
    private final User user;

    /**
     * 인증 유형 (SIGNUP, LOGIN, REJOIN)
     */
    private final AuthType authType;
}
