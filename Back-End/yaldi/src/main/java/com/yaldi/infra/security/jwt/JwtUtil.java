package com.yaldi.infra.security.jwt;
import com.yaldi.infra.security.util.CookieUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 *
 * <p>Access Token과 Refresh Token을 생성하고 검증합니다.
 * Access Token은 서버에 저장하지 않으며, Refresh Token만 Redis에 저장하여 로그아웃 시 무효화합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /** Access Token 쿠키 이름 */
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    /** Refresh Token 쿠키 이름 */
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    /** Authorization 헤더 이름 */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 토큰 접두사 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** JWT 서명용 Secret Key 생성 */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Access Token 생성 (userKey, email 포함, 기본 1시간 유효) */
    public String generateAccessToken(Integer userKey, String email) {
        if (userKey == null || userKey <= 0) {
            throw new IllegalArgumentException("Invalid userKey: must be positive integer");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userKey))  // 사용자 식별자를 subject에 저장
                .claim("email", email)  // 이메일을 클레임에 추가
                .claim("type", "access")  // 토큰 타입 명시
                .issuedAt(now)  // 발급 시각
                .expiration(expiryDate)  // 만료 시각
                .signWith(getSigningKey())  // 비밀 키로 서명
                .compact();
    }

    /** Refresh Token 생성 (userKey 포함, 기본 7일 유효) */
    public String generateRefreshToken(Integer userKey) {
        if (userKey == null || userKey <= 0) {
            throw new IllegalArgumentException("Invalid userKey: must be positive integer");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userKey))  // 사용자 식별자를 subject에 저장
                .claim("type", "refresh")  // 토큰 타입 명시
                .issuedAt(now)  // 발급 시각
                .expiration(expiryDate)  // 만료 시각 (장기)
                .signWith(getSigningKey())  // 비밀 키로 서명
                .compact();
    }

    /** JWT 토큰에서 Claims 추출 */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // Secret Key로 서명 검증
                .build()
                .parseSignedClaims(token)  // 서명된 토큰 파싱
                .getPayload();  // Payload(Claims) 반환
    }

    /** JWT 토큰에서 userKey 추출 */
    public Integer getUserKeyFromToken(String token) {
        return Integer.parseInt(getClaims(token).getSubject());
    }

    /** JWT 토큰에서 email 추출 */
    public String getEmailFromToken(String token) {
        return getClaims(token).get("email", String.class);
    }

    /** JWT 토큰 타입 추출 ("access" or "refresh") */
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }

    /** JWT 토큰 유효성 검증 (서명, 만료시간, 형식) */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())  // Secret Key로 서명 검증
                    .build()
                    .parseSignedClaims(token);  // 파싱 성공 시 유효
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            // 서명이 유효하지 않거나 JWT 형식이 잘못됨
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            // 토큰이 만료됨 (가장 흔한 케이스)
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 JWT 형식
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // JWT 문자열이 비어있거나 null
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /** Authorization Header에서 Bearer 토큰 추출 */
    public Optional<String> extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        // Bearer 접두사가 있는지 확인하고 토큰만 추출
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    /** 쿠키에서 Access Token 추출 */
    public Optional<String> extractAccessTokenFromCookie(HttpServletRequest request) {
        return CookieUtil.getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    /** 쿠키에서 Refresh Token 추출 */
    public Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        return CookieUtil.getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    /** HTTP 요청에서 JWT 추출 (Header 우선, Cookie 대체) */
    public Optional<String> extractToken(HttpServletRequest request) {
        // 1. Authorization Header에서 추출 시도 (모바일/API 클라이언트)
        Optional<String> headerToken = extractTokenFromHeader(request);
        if (headerToken.isPresent()) {
            return headerToken;
        }

        // 2. Cookie에서 추출 시도 (웹 브라우저)
        return extractAccessTokenFromCookie(request);
    }

    /** Access Token 유효 시간 (초 단위) */
    public int getAccessTokenMaxAge() {
        return (int) (jwtProperties.getAccessTokenExpiration() / 1000);
    }

    /** Refresh Token 유효 시간 (초 단위) */
    public int getRefreshTokenMaxAge() {
        return (int) (jwtProperties.getRefreshTokenExpiration() / 1000);
    }
}
