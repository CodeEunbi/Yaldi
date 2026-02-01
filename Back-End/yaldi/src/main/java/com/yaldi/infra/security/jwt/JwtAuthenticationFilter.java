package com.yaldi.infra.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * JWT 기반 인증 필터
 *
 * <h3>필터 동작 원리</h3>
 * <p>모든 HTTP 요청에 대해 JWT 토큰을 검증하고, 유효한 경우 Spring Security의
 * SecurityContext에 인증 정보를 설정합니다. 이를 통해 컨트롤러에서 인증된 사용자 정보에 접근할 수 있습니다.</p>
 *
 * <h3>OncePerRequestFilter 사용 이유</h3>
 * <p>Spring의 Filter는 요청당 여러 번 호출될 수 있습니다(forward, include 등).
 * OncePerRequestFilter를 상속하면 요청당 정확히 한 번만 실행됩니다.</p>
 *
 * <h3>병목현상 완화 기여</h3>
 * <p>이 필터는 JWT 서명 검증만 수행하고 <strong>DB 조회를 하지 않습니다</strong>.
 * Access Token은 서버에 저장하지 않으므로, 매 요청마다 DB나 Redis에 접근하지 않고
 * 빠르게 인증 처리가 가능합니다.</p>
 *
 * <h3>실행 흐름</h3>
 * <ol>
 *   <li>요청에서 JWT 추출 (Authorization Header 또는 Cookie)</li>
 *   <li>JWT 서명 및 만료 시간 검증</li>
 *   <li>유효한 경우 SecurityContext에 인증 정보 설정</li>
 *   <li>다음 필터로 요청 전달</li>
 * </ol>
 *
 * <h3>SecurityConfig 설정 예시</h3>
 * <pre>
 * http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
 * </pre>
 *
 * @author Yaldi Team
 * @version 1.0
 * @see JwtUtil
 * @see OncePerRequestFilter
 * @see SecurityContextHolder
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 모든 HTTP 요청에 대해 JWT 인증을 수행합니다.
     *
     * <p>이 메서드는 요청당 한 번만 실행되며, 다음 작업을 수행합니다:</p>
     * <ol>
     *   <li><strong>토큰 추출:</strong> Authorization Header 또는 Cookie에서 JWT 추출</li>
     *   <li><strong>토큰 검증:</strong> 서명 및 만료 시간 검증 (DB 조회 없음)</li>
     *   <li><strong>인증 설정:</strong> 유효한 경우 SecurityContext에 인증 정보 저장</li>
     * </ol>
     *
     * <h4>인증 실패 처리</h4>
     * <p>토큰이 없거나 유효하지 않은 경우에도 예외를 던지지 않고 다음 필터로 전달합니다.
     * 이는 인증이 필요 없는 엔드포인트(공개 API 등)에 대한 접근을 허용하기 위함입니다.
     * 실제 접근 제어는 SecurityConfig의 authorizeHttpRequests에서 처리됩니다.</p>
     *
     * <h4>SecurityContext 설정</h4>
     * <p>인증 정보가 설정되면 컨트롤러에서 다음과 같이 사용할 수 있습니다:</p>
     * <pre>
     * {@code @GetMapping("/api/user/profile")}
     * public UserProfile getProfile(Authentication authentication) {
     *     Integer userKey = (Integer) authentication.getPrincipal();
     *     return userService.getProfile(userKey);
     * }
     * </pre>
     *
     * @param request HTTP 요청 객체 (JWT 토큰 추출용)
     * @param response HTTP 응답 객체 (사용되지 않음)
     * @param filterChain 다음 필터로 요청을 전달하기 위한 체인
     * @throws ServletException 서블릿 처리 중 오류 발생 시
     * @throws IOException I/O 처리 중 오류 발생 시
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Public 엔드포인트는 JWT 검증을 건너뛰고 바로 통과시킴
        String requestURI = request.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            log.debug("Public endpoint detected, skipping JWT validation: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Header 또는 Cookie에서 Access Token 추출
            Optional<String> accessToken = jwtUtil.extractToken(request);

            // 2. Access Token이 존재하고 유효한 경우 → 인증 처리
            if (accessToken.isPresent() && jwtUtil.validateToken(accessToken.get())) {
                authenticateUser(request, accessToken.get());
            }
            // 3. Access Token이 존재하지만 유효하지 않은 경우 (위조/손상) → 강제 로그아웃
            else if (accessToken.isPresent()) {
                log.warn("Invalid or malformed Access Token detected - forcing logout");

                // 토큰에서 userKey 추출 시도 (손상되지 않은 경우에만 가능)
                try {
                    Integer userKey = jwtUtil.getUserKeyFromToken(accessToken.get());
                    // Redis에서 Refresh Token 삭제 (강제 로그아웃)
                    refreshTokenService.deleteRefreshToken(userKey);
                    log.warn("Forced logout for user {} due to invalid Access Token", userKey);
                } catch (Exception e) {
                    log.warn("Could not extract userKey from invalid token - token is completely malformed");
                }

                // 쿠키 삭제
                CookieUtil.deleteCookie(request, response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME, "/");
                CookieUtil.deleteCookie(request, response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME, "/");

                // 에러 응답 설정
                setErrorResponse(response, ErrorStatus.TOKEN_INVALID_ACCESS);
                return;
            }
            // 4. Access Token이 없는 경우 → Refresh Token으로 자동 갱신 시도
            else {
                Optional<String> refreshToken = jwtUtil.extractRefreshTokenFromCookie(request);

                if (refreshToken.isPresent() && jwtUtil.validateToken(refreshToken.get())) {
                    // Refresh Token에서 userKey 추출
                    Integer userKey = jwtUtil.getUserKeyFromToken(refreshToken.get());

                    // Redis에서 Refresh Token 검증
                    if (refreshTokenService.validateRefreshToken(userKey, refreshToken.get())) {
                        // DB에서 사용자 조회하여 email 가져오기 (활성 사용자만)
                        Optional<User> userOptional = userRepository.findActiveUserById(userKey);

                        if (userOptional.isEmpty()) {
                            log.warn("Deleted user attempted to refresh token: {}", userKey);

                            // Redis에서 Refresh Token 삭제
                            refreshTokenService.deleteRefreshToken(userKey);
                            CookieUtil.deleteCookie(request, response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME, "/");
                            CookieUtil.deleteCookie(request, response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME, "/");

                            // 탈퇴한 사용자 에러 응답
                            setErrorResponse(response, ErrorStatus.USER_DELETED);
                            return;
                        }

                        User user = userOptional.get();

                        // 새로운 Access Token 발급
                        String newAccessToken = jwtUtil.generateAccessToken(userKey, user.getEmail());

                        // 응답 쿠키에 새 Access Token 추가
                        CookieUtil.addCookie(response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME,
                                newAccessToken, jwtUtil.getAccessTokenMaxAge(), "/");

                        // 인증 처리
                        authenticateUser(request, newAccessToken);

                        log.info("Access Token auto-refreshed for user: {}", userKey);
                    } else {
                        log.warn("Invalid Refresh Token in Redis for user: {} - token may have been revoked", userKey);

                        // Refresh Token이 Redis와 불일치 (탈취 가능성) → 강제 로그아웃
                        refreshTokenService.deleteRefreshToken(userKey);
                        CookieUtil.deleteCookie(request, response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME, "/");
                        CookieUtil.deleteCookie(request, response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME, "/");

                        log.warn("Forced logout for user {} due to Refresh Token mismatch", userKey);

                        // 에러 응답 설정
                        setErrorResponse(response, ErrorStatus.TOKEN_MISMATCH_REFRESH);
                        return;
                    }
                } else if (refreshToken.isPresent()) {
                    // Refresh Token이 존재하지만 유효하지 않음 (위조/손상) → 강제 로그아웃
                    log.warn("Invalid or malformed Refresh Token detected - forcing logout");

                    try {
                        Integer userKey = jwtUtil.getUserKeyFromToken(refreshToken.get());
                        refreshTokenService.deleteRefreshToken(userKey);
                        log.warn("Forced logout for user {} due to invalid Refresh Token", userKey);
                    } catch (Exception e) {
                        log.warn("Could not extract userKey from invalid Refresh Token");
                    }

                    CookieUtil.deleteCookie(request, response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME, "/");
                    CookieUtil.deleteCookie(request, response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME, "/");

                    // 에러 응답 설정
                    setErrorResponse(response, ErrorStatus.TOKEN_INVALID_REFRESH);
                    return;
                } else {
                    log.debug("No valid Refresh Token found - authentication required");
                }
            }
        } catch (Exception ex) {
            // JWT 처리 중 예외 발생 시 로그만 남기고 계속 진행
            // 인증 실패는 SecurityConfig의 인가 규칙에서 처리됨
            log.error("Could not set user authentication in security context", ex);
        }

        // 7. 다음 필터로 요청 전달 (인증 성공/실패 무관)
        filterChain.doFilter(request, response);
    }

    /**
     * 공개 엔드포인트 여부를 확인합니다.
     *
     * <p>다음 엔드포인트들은 JWT 인증 없이 접근 가능합니다:</p>
     * <ul>
     *   <li>/api/v1/health - 헬스 체크</li>
     *   <li>/swagger-ui/** - Swagger UI</li>
     *   <li>/v3/api-docs/** - OpenAPI 문서</li>
     *   <li>/actuator/** - Spring Actuator</li>
     * </ul>
     *
     * @param uri 요청 URI
     * @return 공개 엔드포인트이면 true, 아니면 false
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/v1/health") ||
                uri.startsWith("/swagger-ui") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/actuator");
    }

    /**
     * SecurityContext에 사용자 인증 정보를 설정합니다.
     *
     * @param request HTTP 요청 객체
     * @param token 유효한 JWT 토큰
     */
    private void authenticateUser(HttpServletRequest request, String token) {
        Integer userKey = jwtUtil.getUserKeyFromToken(token);

        // UsernamePasswordAuthenticationToken 생성 (권한 없음)
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userKey, null, new ArrayList<>());

        // 요청 세부 정보 설정 (IP, 세션 ID 등)
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // SecurityContext에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Set authentication for user: {}", userKey);
    }

    /**
     * 에러 응답을 JSON 형식으로 설정합니다.
     *
     * @param response HTTP 응답 객체
     * @param errorStatus 에러 상태 정보
     * @throws IOException JSON 변환 중 오류 발생 시
     */
    private void setErrorResponse(HttpServletResponse response, ErrorStatus errorStatus) throws IOException {
        response.setStatus(errorStatus.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Object> errorResponse = ApiResponse.onFailure(errorStatus, null);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}