package com.yaldi.domain.auth.controller;

import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.jwt.RefreshTokenService;
import com.yaldi.infra.security.util.CookieUtil;
import com.yaldi.infra.security.jwt.JwtUtil;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 인증 관련 API
 *
 * <p>JWT 토큰 갱신, 로그아웃 등 인증 관련 엔드포인트를 제공합니다.</p>
 */
@Tag(name = "Auth", description = "인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 로그아웃
     *
     * <p>다음 작업을 수행합니다:</p>
     * <ol>
     *   <li>Redis에서 Refresh Token 삭제 (즉시 무효화)</li>
     *   <li>쿠키에서 Access Token 및 Refresh Token 삭제</li>
     *   <li>SecurityContext 초기화</li>
     * </ol>
     *
     * <p><strong>주의:</strong> Access Token은 서버에 저장하지 않으므로,
     * 쿠키가 삭제되더라도 토큰 문자열을 복사한 경우 만료 시간까지 기술적으로 유효합니다.
     * 하지만 Refresh Token이 Redis에서 삭제되므로 갱신이 불가능하며,
     * 최대 1시간 후에는 완전히 무효화됩니다.</p>
     */
    @Operation(
        summary = "로그아웃",
        description = "현재 로그인한 사용자를 로그아웃합니다. " +
                      "Redis에서 Refresh Token을 삭제하고, 쿠키를 모두 제거하며, SecurityContext를 초기화합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<?> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. 현재 인증된 사용자 정보 조회
        if (SecurityUtil.isAuthenticated()) {
            Integer userKey = SecurityUtil.getCurrentUserKey();

            // 2. Redis에서 Refresh Token 삭제 (즉시 무효화)
            refreshTokenService.deleteRefreshToken(userKey);

            log.info("User logged out: userKey={}", userKey);
        }

        // 3. 쿠키 삭제 (생성 시와 동일한 경로로 삭제해야 함)
        CookieUtil.deleteCookie(request, response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME, "/");
        CookieUtil.deleteCookie(request, response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME, "/");

        // 4. SecurityContext 초기화
        SecurityContextHolder.clearContext();

        return ApiResponse.OK;
    }

    /**
     * Refresh Token으로 Access Token 갱신 (Refresh Token Rotation 적용)
     *
     * <p>보안 강화를 위해 Refresh Token Rotation을 적용합니다:</p>
     * <ol>
     *   <li>기존 Refresh Token 검증</li>
     *   <li>새로운 Access Token + Refresh Token 발급</li>
     *   <li>기존 Refresh Token은 Redis Blacklist에 추가하여 재사용 방지</li>
     * </ol>
     *
     * <p><strong>Refresh Token Rotation의 보안 장점:</strong></p>
     * <ul>
     *   <li>토큰 탈취 시 재사용 불가능</li>
     *   <li>토큰 탈취 감지 가능 (같은 Refresh Token이 2번 사용되면 의심)</li>
     *   <li>공격자가 토큰을 재사용하려고 하면 즉시 강제 로그아웃</li>
     * </ul>
     */
    @Operation(
        summary = "토큰 갱신 (Refresh Token Rotation)",
        description = "Refresh Token으로 새로운 Access Token과 Refresh Token을 발급받습니다. " +
                      "보안을 위해 기존 Refresh Token은 Redis Blacklist에 추가되어 재사용이 방지됩니다."
    )
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = jwtUtil.extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TOKEN_NOT_FOUND_REFRESH));

        // 2. Refresh Token 유효성 검증 (서명, 만료 시간)
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new GeneralException(ErrorStatus.TOKEN_INVALID_REFRESH);
        }

        // 3. Refresh Token에서 userKey 추출
        Integer userKey = jwtUtil.getUserKeyFromToken(refreshToken);

        // 4. Redis에 저장된 Refresh Token과 일치하는지 검증
        if (!refreshTokenService.validateRefreshToken(userKey, refreshToken)) {
            // Redis와 불일치 → 이미 사용된 토큰이거나 탈취된 토큰
            log.warn("Refresh Token mismatch detected for user: {} - possible token theft!", userKey);

            // 강제 로그아웃 (보안 강화)
            refreshTokenService.deleteRefreshToken(userKey);
            CookieUtil.deleteCookie(request, response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME, "/");
            CookieUtil.deleteCookie(request, response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME, "/");

            throw new GeneralException(ErrorStatus.TOKEN_MISMATCH_REFRESH);
        }

        // 5. DB에서 사용자 정보 조회 (활성 사용자만)
        User user = userRepository.findActiveUserById(userKey)
                .orElseThrow(() -> {
                    log.warn("Deleted user attempted to refresh token: {}", userKey);
                    refreshTokenService.deleteRefreshToken(userKey);
                    return new GeneralException(ErrorStatus.USER_DELETED);
                });

        // 6. 새로운 Access Token + Refresh Token 발급
        String newAccessToken = jwtUtil.generateAccessToken(userKey, user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(userKey);

        // 7. 기존 Refresh Token을 Redis Blacklist에 추가 (재사용 방지)
        // Blacklist TTL = 기존 Refresh Token의 남은 유효 시간
        Long remainingTTL = refreshTokenService.getRefreshTokenTTL(userKey);
        if (remainingTTL != null && remainingTTL > 0) {
            String blacklistKey = "refresh_token:blacklist:" + refreshToken;
            redisTemplate.opsForValue().set(
                    blacklistKey,
                    "revoked",
                    remainingTTL,
                    TimeUnit.SECONDS
            );
            log.info("Old Refresh Token added to blacklist for user: {}, TTL: {}s", userKey, remainingTTL);
        }

        // 8. Redis에 새로운 Refresh Token 저장 (기존 토큰 덮어쓰기)
        refreshTokenService.saveRefreshToken(userKey, newRefreshToken);

        // 9. 쿠키에 새 토큰 설정
        CookieUtil.addCookie(response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME,
                newAccessToken, jwtUtil.getAccessTokenMaxAge(), "/");
        CookieUtil.addCookie(response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME,
                newRefreshToken, jwtUtil.getRefreshTokenMaxAge(), "/");

        log.info("Token refreshed successfully for user: {} (Refresh Token Rotation applied)", userKey);

        return ApiResponse.onSuccess(TokenRefreshResponse.builder()
                .message("Token refreshed successfully")
                .accessTokenExpiresIn(jwtUtil.getAccessTokenMaxAge())
                .refreshTokenExpiresIn(jwtUtil.getRefreshTokenMaxAge())
                .build());
    }

    /**
     * 토큰 갱신 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class TokenRefreshResponse {
        private String message;
        private Integer accessTokenExpiresIn;  // Access Token 유효 시간 (초)
        private Integer refreshTokenExpiresIn;  // Refresh Token 유효 시간 (초)
    }
}
