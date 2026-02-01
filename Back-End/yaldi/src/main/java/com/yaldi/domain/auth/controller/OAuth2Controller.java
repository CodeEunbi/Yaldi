package com.yaldi.domain.auth.controller;

import com.yaldi.infra.security.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * OAuth2 로그인을 위한 래퍼 컨트롤러
 *
 * <p>개발 환경과 운영 환경에서 동적으로 리다이렉트 타겟을 설정할 수 있도록 합니다.</p>
 * <p>Origin 헤더를 기반으로 리다이렉트 타겟을 결정하고, 쿠키에 저장한 후 Spring Security의 OAuth2 엔드포인트로 리다이렉트합니다.</p>
 */
@Tag(name = "OAuth2", description = "OAuth2 로그인 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/oauth2")
public class OAuth2Controller {

    public static final String OAUTH2_REDIRECT_TARGET_COOKIE_NAME = "oauth2_redirect_target";
    private static final int COOKIE_MAX_AGE = 180; // 3분 (OAuth2 인증 완료까지 충분한 시간)

    // 허용된 origin 목록 (보안을 위한 화이트리스트 - Open Redirect 방지)
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://yaldi.kr"
    );

    /**
     * OAuth2 로그인 시작 (공통 엔드포인트)
     *
     * <p>Spring Security에 설정된 OAuth2 Provider로 리다이렉트하는 래퍼 엔드포인트입니다.</p>
     * <p>Origin 헤더를 기반으로 리다이렉트 타겟을 결정하고, 쿠키에 저장한 후 Spring Security의 OAuth2 엔드포인트로 리다이렉트합니다.</p>
     *
     * @param provider OAuth2 Provider 이름
     * @param redirectTarget 명시적으로 지정할 리다이렉트 타겟 (선택적, 쿼리 파라미터)
     * @param mode 팝업 모드 여부 (popup)
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    @Operation(
            summary = "OAuth2 로그인",
            description = "OAuth2 로그인을 시작합니다. Origin 헤더 또는 redirect_target 파라미터를 기반으로 로그인 후 리다이렉트될 URL을 결정합니다."
    )
    @GetMapping("/authorization/{provider}")
    public void oauth2Login(
            @Parameter(description = "OAuth2 Provider 이름", required = true)
            @PathVariable("provider") String provider,
            @Parameter(description = "명시적으로 지정할 리다이렉트 타겟 (선택적)")
            @RequestParam(value = "redirect_target", required = false) String redirectTarget,
            @Parameter(description = "팝업 모드 여부 (popup)")
            @RequestParam(value = "mode", required = false) String mode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // 1. Origin 헤더 또는 Referer 헤더 또는 redirect_target 파라미터에서 리다이렉트 타겟 결정
        String origin = redirectTarget != null ? redirectTarget : extractOriginFromRequest(request);
        String finalRedirectTarget = decideRedirectTarget(origin);

        if (finalRedirectTarget == null) {
            log.warn("Invalid origin or redirect_target: {} (Origin: {}, Referer: {})",
                    origin, request.getHeader("Origin"), request.getHeader("Referer"));
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid origin or redirect_target. Allowed origins: " + String.join(", ", ALLOWED_ORIGINS));
            return;
        }

        // 2. 쿠키에 리다이렉트 타겟 저장 (OAuth2 인증 완료 후 사용)
        // CookieUtil을 사용하여 환경별 설정 자동 적용 (secure, sameSite 등)
        CookieUtil.addCookie(response, OAUTH2_REDIRECT_TARGET_COOKIE_NAME,
                finalRedirectTarget, COOKIE_MAX_AGE, "/", "High");

        log.info("OAuth2 login initiated for provider: {} with redirect_target: {}", provider, finalRedirectTarget);

        // 3. Spring Security의 OAuth2 엔드포인트로 리다이렉트
        String oauth2Url = "/oauth2/authorization/" + provider;
        if (mode != null && "popup".equals(mode)) {
            oauth2Url += "?mode=popup";
        }

        response.sendRedirect(oauth2Url);
    }

    /**
     * Request에서 Origin을 추출합니다.
     * Origin 헤더가 없으면 Referer 헤더에서 추출합니다.
     *
     * @param request HttpServletRequest
     * @return Origin (예: http://localhost:5173 또는 https://yaldi.kr)
     */
    private String extractOriginFromRequest(HttpServletRequest request) {
        // 1. Origin 헤더 확인 (CORS 요청, Fetch API 등)
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.trim().isEmpty()) {
            return origin;
        }

        // 2. Referer 헤더에서 추출 (일반 네비게이션)
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            try {
                java.net.URI uri = java.net.URI.create(referer);
                return uri.getScheme() + "://" + uri.getAuthority();
            } catch (Exception e) {
                log.warn("Failed to extract origin from Referer: {}", referer, e);
            }
        }

        return null;
    }

    /**
     * Origin을 기반으로 리다이렉트 타겟을 결정합니다.
     *
     * <p>화이트리스트 기반으로만 허용하여 Open Redirect 취약점을 방지합니다.</p>
     *
     * @param origin Origin 헤더 또는 redirect_target 파라미터 값
     * @return 리다이렉트 타겟 URL (화이트리스트에 없으면 null)
     */
    private String decideRedirectTarget(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return null;
        }

        // 화이트리스트 검증 (Open Redirect 방지)
        if (!ALLOWED_ORIGINS.contains(origin)) {
            return null;
        }

        // /oauth2/redirect 경로 추가
        return origin + "/oauth2/redirect";
    }
}


