package com.yaldi.domain.user.controller;

import com.yaldi.domain.user.dto.SocialAccountListResponse;
import com.yaldi.domain.user.dto.UpdateNicknameRequest;
import com.yaldi.domain.user.dto.UserResponse;
import com.yaldi.domain.user.service.UserService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.security.jwt.JwtUtil;
import com.yaldi.infra.security.util.CookieUtil;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관리 API
 */
@Tag(name = "User", description = "사용자 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<?> getMyInfo() {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        UserResponse userInfo = userService.getUserInfo(userKey);

        return ApiResponse.onSuccess(userInfo);
    }

    /**
     * 현재 로그인한 사용자의 소셜 계정 목록 조회
     */
    @Operation(summary = "내 소셜 계정 목록 조회", description = "현재 로그인한 사용자에 연결된 소셜 계정 목록을 조회합니다.")
    @GetMapping("/me/social-accounts")
    public ApiResponse<?> getMySocialAccounts() {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        SocialAccountListResponse socialAccounts = userService.getUserSocialAccounts(userKey);

        return ApiResponse.onSuccess(socialAccounts);
    }

    /**
     * 닉네임 변경
     */
    @Operation(summary = "닉네임 변경", description = "현재 로그인한 사용자의 닉네임을 변경합니다.")
    @PatchMapping("/me/nickname")
    public ApiResponse<?> updateMyNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        userService.updateNickname(userKey, request);

        return ApiResponse.OK;
    }

    /**
     * 회원 탈퇴 (Soft Delete)
     *
     * <p>회원 탈퇴 시 다음 작업을 수행합니다:</p>
     * <ul>
     *   <li>사용자 데이터 Soft Delete</li>
     *   <li>Redis에서 Refresh Token 삭제</li>
     *   <li>쿠키에서 Access Token 및 Refresh Token 삭제</li>
     *   <li>SecurityContext 초기화 (즉시 로그아웃)</li>
     * </ul>
     */
    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자를 삭제합니다 (Soft Delete). 모든 토큰이 삭제되며 즉시 로그아웃됩니다.")
    @DeleteMapping("/me")
    public ApiResponse<?> deleteMyAccount(
            HttpServletRequest request,
            HttpServletResponse response) {
        Integer userKey = SecurityUtil.getCurrentUserKey();

        // 1. 사용자 삭제 (Soft Delete + Refresh Token 삭제)
        userService.deleteUser(userKey);

        // 2. 쿠키 삭제 (생성 시와 동일한 경로로 삭제해야 함)
        CookieUtil.deleteCookie(request, response, JwtUtil.ACCESS_TOKEN_COOKIE_NAME, "/");
        CookieUtil.deleteCookie(request, response, JwtUtil.REFRESH_TOKEN_COOKIE_NAME, "/");

        // 3. SecurityContext 초기화
        SecurityContextHolder.clearContext();

        log.info("User account deleted and logged out: userKey={}", userKey);

        return ApiResponse.OK;
    }
}
