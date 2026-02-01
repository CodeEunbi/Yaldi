package com.yaldi.infra.security.oauth2.handler;

import com.yaldi.domain.auth.controller.OAuth2Controller;
import com.yaldi.domain.user.entity.AuthType;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.jwt.RefreshTokenService;
import com.yaldi.infra.security.util.CookieUtil;
import com.yaldi.infra.security.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * OAuth2 로그인 성공 후 JWT 토큰을 발급하고 쿠키에 저장하는 핸들러
 *
 * <p>JWT 토큰(Access/Refresh)을 생성하여 HttpOnly 쿠키에 저장하고,
 * Refresh Token은 Redis에 저장하여 로그아웃 시 무효화할 수 있도록 합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    /**
     * OAuth2 인증 성공 후 리다이렉트할 프론트엔드 URL
     * application.yml의 oauth2.authorized-redirect-uris 설정 값
     */
    @Value("${oauth2.authorized-redirect-uris}")
    private String redirectUri;

    // SecurityConfig의 CORS 설정과 동일하게 유지
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "https://yaldi.kr"
    );

    /**
     * OAuth2 로그인 성공 시 JWT 토큰을 발급하고 쿠키에 저장합니다.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // 1. 응답이 이미 커밋되었는지 확인 (타임아웃 등의 상황)
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", redirectUri);
            return;
        }

        // 2. OAuth2 인증 결과에서 사용자 정보 추출
        // OIDC 사용 시 DefaultOidcUser가 반환되므로 OAuth2User로 받아서 처리
        org.springframework.security.oauth2.core.user.OAuth2User oAuth2User =
                (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();

        // 3. Request Attribute에서 이메일 가져오기 (CustomOAuth2UserService에서 저장한 값)
        // GitHub 등 일부 provider는 attributes에 email이 없을 수 있으므로 Request Attribute 우선 사용
        String email = (String) request.getAttribute("userEmail");

        // Request Attribute에 없으면 OAuth2User attributes에서 가져오기 (Google 등)
        if (email == null || email.trim().isEmpty()) {
            email = oAuth2User.getAttribute("email");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalStateException("Email not provided by OAuth2 provider");
        }

        // 4. DB에서 사용자 조회 (CustomOAuth2UserService에서 이미 저장됨)
        com.yaldi.domain.user.entity.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 탈퇴한 사용자 확인
        if (user.getDeletedAt() != null) {
            throw new GeneralException(ErrorStatus.USER_DELETED);
        }

        // 5. JWT 토큰 생성
        // Access Token: API 요청 시 사용 (짧은 유효기간)
        String accessToken = jwtUtil.generateAccessToken(
                user.getUserKey(),
                user.getEmail()
        );

        // Refresh Token: Access Token 갱신용 (긴 유효기간)
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserKey());

        // 6. Refresh Token을 Redis에 저장
        // Access Token은 저장하지 않아 병목현상 완화
        // 로그아웃 시 Redis에서 삭제하여 토큰 무효화 가능
        refreshTokenService.saveRefreshToken(user.getUserKey(), refreshToken);

        // 7. 쿠키에 토큰 저장 (보안을 위해 HttpOnly 설정)
        // HttpOnly: JavaScript에서 접근 불가 (XSS 공격 방어)
        // Secure: HTTPS에서만 전송 (Production 환경에서 활성화 필요)

        // Access Token: 모든 경로에서 전송 (Path="/")
        CookieUtil.addCookie(response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME,
                accessToken, jwtUtil.getAccessTokenMaxAge(), "/");

        // Refresh Token: 모든 경로에서 전송 (Access Token 자동 갱신을 위해 필요)
        // JWT 필터에서 Access Token 만료 시 Refresh Token으로 자동 갱신하므로 모든 요청에서 필요
        CookieUtil.addCookie(response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME,
                refreshToken, jwtUtil.getRefreshTokenMaxAge(), "/");

        // 8. Request Attribute에서 AuthType 가져오기
        AuthType authType = (AuthType) request.getAttribute("authType");
        if (authType == null) {
            authType = AuthType.LOGIN; // 기본값 (안전장치)
        }

        // 9. 쿠키에서 리다이렉트 타겟 읽기 (OAuth2Controller에서 저장한 값)
        String targetUrl = resolveRedirectTargetFromCookie(request, response);

        // 10. 프론트엔드로 리다이렉트 (authType을 쿼리 파라미터로 전달)
        // 토큰은 쿠키에 저장되어 있으므로 URL에 포함하지 않음
        // authType: 프론트엔드에서 신규 가입/로그인/재가입을 구분하여 처리

        // 팝업 모드인지 확인 (쿼리 파라미터로 구분)
        String mode = request.getParameter("mode");

        if ("popup".equals(mode)) {
            // [1] 팝업 모드: PostMessage로 부모 창에 전달
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(
                    "<html><body><script>" +
                            "window.opener.postMessage({" +
                            "  authType: '" + authType.name() + "'," +
                            "  success: true" +
                            "}, '" + extractOriginFromUrl(targetUrl) + "');" +
                            "window.close();" +
                            "</script></body></html>"
            );

            log.info("OAuth2 login success for user /w `popup`: {}, authType: {}",
                    user.getEmail(), authType);
        } else {
            // [2] 일반 모드: 리다이렉트
            String finalTargetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("authType", authType.name())
                    .build()
                    .toUriString();

            log.info("OAuth2 login success for user: {}, authType: {}. Redirecting to: {}",
                    user.getEmail(), authType, finalTargetUrl);

            getRedirectStrategy().sendRedirect(request, response, finalTargetUrl);
        }
    }

    /**
     * 쿠키에서 리다이렉트 타겟을 읽어옵니다.
     * 쿠키가 없거나 유효하지 않으면 기본값(application.yml의 설정)을 사용합니다.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse (쿠키 삭제용)
     * @return 리다이렉트 타겟 URL
     */
    private String resolveRedirectTargetFromCookie(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 리다이렉트 타겟 읽기
        String redirectTarget = null;
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            redirectTarget = Arrays.stream(cookies)
                    .filter(cookie -> OAuth2Controller.OAUTH2_REDIRECT_TARGET_COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        // 쿠키 삭제 (일회용)
        if (redirectTarget != null) {
            CookieUtil.deleteCookie(request, response, OAuth2Controller.OAUTH2_REDIRECT_TARGET_COOKIE_NAME, "/");
            log.debug("Resolved redirect target from cookie: {}", redirectTarget);
        } else {
            // 쿠키가 없으면 기본값 사용 (application.yml의 oauth2.authorized-redirect-uris)
            redirectTarget = redirectUri;
            log.debug("No redirect target cookie found, using default: {}", redirectTarget);
        }

        return redirectTarget;
    }

    /**
     * URL에서 Origin을 추출합니다.
     *
     * @param url 전체 URL (예: http://localhost:5173/oauth2/redirect)
     * @return Origin (예: http://localhost:5173)
     */
    private String extractOriginFromUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String origin = uri.getScheme() + "://" + uri.getAuthority();

            // ALLOWED_ORIGINS에 포함되어 있는지 확인
            if (ALLOWED_ORIGINS.contains(origin)) {
                return origin;
            }
        } catch (Exception e) {
            log.warn("Failed to extract origin from URL: {}", url, e);
        }

        // 실패 시 기본값 반환
        return ALLOWED_ORIGINS.get(0);
    }
}