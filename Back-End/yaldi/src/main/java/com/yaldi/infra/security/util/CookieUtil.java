package com.yaldi.infra.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

/**
 * HTTP Cookie 관리 유틸리티
 *
 * <p>JWT 토큰을 HttpOnly 쿠키에 저장하여 XSS 공격으로부터 보호합니다.</p>
 */
@Component
public class CookieUtil {

    /** Cookie Secure 플래그 활성화 여부 (application.yml에서 설정) */
    private static boolean secureEnabled;

    /** Cookie SameSite 속성 (application.yml에서 설정, 기본값: Lax) */
    private static String sameSite;

    /** Cookie Domain 속성 (application.yml에서 설정, 빈 값이면 설정 안함) */
    private static String domain;

    @Value("${cookie.secure:false}")
    public void setSecureEnabled(boolean secure) {
        CookieUtil.secureEnabled = secure;
    }

    @Value("${cookie.same-site:Lax}")
    public void setSameSite(String sameSite) {
        CookieUtil.sameSite = sameSite;
    }

    @Value("${cookie.domain:}")
    public void setDomain(String domain) {
        // 빈 문자열이면 null로 저장 (domain 설정 안함)
        CookieUtil.domain = (domain == null || domain.trim().isEmpty()) ? null : domain;
    }

    /**
     * HTTP 응답에 쿠키를 추가합니다.
     * HttpOnly, Secure(환경변수 설정 시) 플래그가 적용됩니다.
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        addCookie(response, name, value, maxAge, "/");
    }

    /**
     * HTTP 응답에 쿠키를 추가합니다 (경로 지정 가능).
     * HttpOnly, Secure(환경변수 설정 시), SameSite, Priority 플래그가 적용됩니다.
     *
     * @param response HTTP 응답
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초)
     * @param path 쿠키 경로 (예: "/", "/api/auth/refresh")
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, String path) {
        addCookie(response, name, value, maxAge, path, "High");
    }

    /**
     * HTTP 응답에 쿠키를 추가합니다 (우선순위 지정 가능).
     * HttpOnly, Secure(환경변수 설정 시), SameSite, Priority 플래그가 적용됩니다.
     *
     * @param response HTTP 응답
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초)
     * @param path 쿠키 경로 (예: "/", "/api/auth/refresh")
     * @param priority 쿠키 우선순위 ("High", "Medium", "Low")
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge, String path, String priority) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path(path)
                .httpOnly(true)         // XSS 방어
                .maxAge(maxAge)
                .secure(secureEnabled)  // 환경별 설정 (prod: true, dev: false)
                .sameSite(sameSite);    // 환경별 설정 (기본: Lax)

        // domain이 설정되어 있으면 추가, 없으면 설정 안함 (브라우저가 자동으로 요청한 호스트 사용)
        if (domain != null) {
            builder.domain(domain);
        }

        ResponseCookie cookie = builder.build();

        // Priority 속성 추가 (Set-Cookie 헤더에 직접 추가)
        String cookieHeader = cookie.toString() + "; Priority=" + priority;
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /** 요청에서 특정 이름의 쿠키 조회 */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        // 쿠키가 없는 경우 빈 Optional 반환
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    /** 요청에서 쿠키 값 조회 */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        return getCookie(request, name)
                .map(Cookie::getValue);
    }

    /** 쿠키 삭제 (MaxAge=0으로 즉시 만료) */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        deleteCookie(request, response, name, "/");
    }

    /** 쿠키 삭제 (경로 지정 가능, MaxAge=0으로 즉시 만료) */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name, String path) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    // 쿠키를 삭제하기 위해 값을 비우고 MaxAge를 0으로 설정
                    cookie.setValue("");
                    cookie.setPath(path);  // 생성 시와 동일한 Path 사용
                    cookie.setMaxAge(0);  // 즉시 만료
                    response.addCookie(cookie);
                }
            }
        }
    }

    /** 객체를 Base64로 직렬화 */
    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    /** Base64 쿠키 값을 객체로 역직렬화 */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}
