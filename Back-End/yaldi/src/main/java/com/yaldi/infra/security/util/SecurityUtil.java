package com.yaldi.infra.security.util;

import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security Context에서 현재 사용자 정보를 가져오는 유틸리티
 */
@Slf4j
public class SecurityUtil {

    private SecurityUtil() {
        // Utility class - 인스턴스화 방지
    }

    /**
     * 현재 인증된 사용자의 Authentication 객체를 가져옵니다
     *
     * @return Authentication 객체
     * @throws GeneralException 인증되지 않은 경우
     */
    public static Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            "anonymousUser".equals(authentication.getPrincipal())) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }

        return authentication;
    }

    /**
     * 현재 인증된 사용자의 userKey를 가져옵니다
     *
     * <p>JWT 토큰의 subject에서 userKey를 추출합니다.</p>
     *
     * @return 사용자 ID (userKey)
     * @throws GeneralException 인증되지 않았거나 userKey를 파싱할 수 없는 경우
     */
    public static Integer getCurrentUserKey() {
        Authentication authentication = getCurrentAuthentication();

        try {
            // JWT 필터에서 설정한 principal (userKey)
            String userKeyStr = authentication.getName();
            return Integer.parseInt(userKeyStr);
        } catch (NumberFormatException e) {
            log.error("Failed to parse userKey from authentication: {}", authentication.getName(), e);
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
    }

    /**
     * 현재 인증된 사용자의 이메일을 가져옵니다
     *
     * <p>JWT 토큰의 claims에서 email을 추출합니다.</p>
     *
     * @return 사용자 이메일
     * @throws GeneralException 인증되지 않았거나 이메일을 찾을 수 없는 경우
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = getCurrentAuthentication();

        // JWT 필터에서 설정한 details (email)
        Object details = authentication.getDetails();
        if (details instanceof String) {
            return (String) details;
        }

        log.warn("Email not found in authentication details");
        return null;
    }

    /**
     * 현재 사용자가 인증되었는지 확인합니다
     *
     * @return 인증된 경우 true
     */
    public static boolean isAuthenticated() {
        try {
            getCurrentAuthentication();
            return true;
        } catch (GeneralException e) {
            return false;
        }
    }
}
