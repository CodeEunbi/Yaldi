package com.yaldi.infra.security.oauth2.handler;

import com.yaldi.domain.auth.controller.OAuth2Controller;
import com.yaldi.infra.security.util.CookieUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${oauth2.authorized-redirect-uris}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        // 쿠키에서 리다이렉트 타겟 읽기 (OAuth2Controller에서 저장한 값)
        String redirectTarget = resolveRedirectTargetFromCookie(request, response);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectTarget)
                .queryParam("error", exception.getLocalizedMessage())
                .build()
                .toUriString();
log.error(exception.toString());
        log.error("OAuth2 login failed: {}. Redirecting to: {}", exception.getMessage(), targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 쿠키에서 리다이렉트 타겟을 읽어옵니다.
     * 쿠키가 없으면 기본값(application.yml의 설정)을 사용합니다.
     *
     * @param request HttpServletRequest
     * @param response HttpServletResponse (쿠키 삭제용)
     * @return 리다이렉트 타겟 URL
     */
    private String resolveRedirectTargetFromCookie(HttpServletRequest request, HttpServletResponse response) {
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
            // 쿠키가 없으면 기본값 사용
            redirectTarget = redirectUri;
            log.debug("No redirect target cookie found, using default: {}", redirectTarget);
        }

        return redirectTarget;
    }
}
