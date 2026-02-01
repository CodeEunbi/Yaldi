package com.yaldi.domain.auth.controller;

import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.jwt.RefreshTokenService;
import com.yaldi.infra.security.util.CookieUtil;
import com.yaldi.infra.security.jwt.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 개발용 Mock 사용자 인증 컨트롤러
 *
 * <p>이 컨트롤러는 개발 환경에서만 활성화되며, OAuth 없이 빠르게 회원가입/로그인할 수 있습니다.</p>
 */
@Tag(name = "* Dev Auth", description = "인증 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/dev/auth")
@RequiredArgsConstructor
@Profile({"dev", "local", "prod"})  // dev 또는 local 프로파일에서만 활성화, prod
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * Mock 사용자 목록 조회
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<MockUserDto>>> getMockUsers() {
        List<MockUserDto> users = userRepository.findAllIncludingDeleted().stream()
                .limit(8)  // Mock 데이터 1-8번 사용자
                .map(MockUserDto::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.onSuccess(users));
    }

    /**
     * Mock 사용자로 로그인 (탈퇴한 사용자도 복구하여 로그인)
     *
     * @param userKey 사용자 키
     * @param response HTTP 응답 (쿠키 설정용)
     */
    @Transactional
    @PostMapping("/login/{userKey}")
    public ResponseEntity<ApiResponse<LoginResponse>> loginAsMockUser(
            @PathVariable Integer userKey,
            HttpServletResponse response
    ) {
        // 사용자 조회 (삭제된 경우 자동 복구)
        User user = userRepository.findById(userKey)
                .orElseGet(() -> {
                    // 없으면 삭제된 사용자니까 복구하고 다시 조회
                    userRepository.restoreDeletedUser(userKey);
                    User restored = userRepository.findById(userKey)
                            .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
                    log.info("Dev restored deleted user: {} ({})", restored.getNickname(), restored.getEmail());
                    return restored;
                });

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getUserKey(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserKey());

        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(user.getUserKey(), refreshToken);

        // 쿠키에 토큰 저장
        CookieUtil.addCookie(response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME,
                accessToken, jwtUtil.getAccessTokenMaxAge(), "/");
        CookieUtil.addCookie(response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME,
                refreshToken, jwtUtil.getRefreshTokenMaxAge(), "/");

        log.info("Dev login success for mock user: {} ({})", user.getNickname(), user.getEmail());

        LoginResponse loginResponse = new LoginResponse(
                user.getUserKey(),
                user.getEmail(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                "Mock 사용자로 로그인되었습니다."
        );

        return ResponseEntity.ok(ApiResponse.onSuccess(loginResponse));
    }

    /**
     * Mock 사용자 DTO
     */
    public record MockUserDto(
            Integer userKey,
            String email,
            String nickname,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime deletedAt
    ) {
        public static MockUserDto from(User user) {
            return new MockUserDto(
                    user.getUserKey(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getCreatedAt(),
                    user.getUpdatedAt(),
                    user.getDeletedAt()
            );
        }
    }

    /**
     * 로그인 응답 DTO
     */
    public record LoginResponse(
            Integer userKey,
            String email,
            String nickname,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String message
    ) {}
}