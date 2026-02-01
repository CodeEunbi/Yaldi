package com.yaldi.domain.user.entity;

/**
 * OAuth2 인증 유형
 *
 * <p>OAuth2 로그인 시 사용자의 가입/로그인 상태를 구분합니다.</p>
 *
 * @author Yaldi Team
 * @version 1.0
 */
public enum AuthType {
    /**
     * 신규 가입 - 완전히 새로운 사용자
     */
    SIGNUP,

    /**
     * 로그인 - 기존 활성 사용자
     */
    LOGIN,

    /**
     * 재가입 - 탈퇴했던 사용자가 다시 가입
     */
    REJOIN
}
