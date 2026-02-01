package com.yaldi.infra.security.config;

import com.yaldi.infra.security.jwt.JwtAuthenticationFilter;
import com.yaldi.infra.security.oauth2.CustomOAuth2UserService;
import com.yaldi.infra.security.oauth2.CustomOidcUserService;
import com.yaldi.infra.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.yaldi.infra.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 비활성화 (JWT 사용)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Public 엔드포인트
                        .requestMatchers(
                                "**",
                                "/**",
                                "/ws",
                                "/ws/**",
                                "/api/v1/health/**",
                                "/api/v1/viewer/*/stream",  // 뷰어 SSE 스트리밍 (비회원 접근 가능)
                                "/api/v1/viewer/*/validate",  // 뷰어링크 검증 (비회원 접근 가능)
                                "/api/v1/viewer/test/**",  // 뷰어 테스트 API (개발 환경 전용)
                                "/swagger",
                                "/swagger/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/api/dev/auth/**",  // 개발용 로그인 API (dev, local 프로파일에서만 활성화됨)
                                "/api/kafka/**",
                                "/error"  // Spring Boot 기본 에러 페이지
                        ).permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )

                // 인증 예외 처리 (API 요청은 401 반환, 브라우저는 OAuth2 로그인 페이지로)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API 요청인 경우 401 반환
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(401);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"isSuccess\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}");
                            } else {
                                // 브라우저 요청은 OAuth2 로그인 페이지로 리다이렉트
                                response.sendRedirect("/oauth2/authorization/google");
                            }
                        })
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                // OAuth2 UserService (일반 OAuth2)
                                .userService(customOAuth2UserService)
                                // OIDC UserService (openid scope 사용 시)
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin (프론트엔드 주소)
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "https://yaldi.kr",
                "http://127.0.0.1:5500",  // Live Server
                "http://localhost:5500"));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // 자격증명 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
