package com.yaldi.domain.version.controller;

import com.yaldi.domain.version.dto.request.UpdateVersionRequest;
import com.yaldi.domain.version.dto.request.UpdateVersionVisibilityRequest;
import com.yaldi.domain.version.dto.response.VersionResponse;
import com.yaldi.domain.version.dto.response.compare.VersionCompareResponse;
import com.yaldi.domain.version.entity.Version;
import com.yaldi.domain.version.service.VersionService;
import com.yaldi.domain.version.service.VersionCompareService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Version", description = "버전 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
@Validated
public class VersionController {

    private final VersionService versionService;
    private final VersionCompareService versionCompareService;

    @Operation(summary = "버전 상세 조회", description = "특정 버전의 상세 정보를 조회합니다")
    @GetMapping("/{versionKey}")
    public ApiResponse<VersionResponse> getVersion(
            @Parameter(description = "버전 ID", required = true)
            @PathVariable @Min(value = 1, message = "버전 ID는 1 이상이어야 합니다") Long versionKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        VersionResponse response = versionService.getVersion(userKey, versionKey);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "버전 수정", description = "버전의 이름과 설명을 수정합니다")
    @PatchMapping("/{versionKey}")
    public ApiResponse<VersionResponse> updateVersion(
            @Parameter(description = "버전 ID", required = true)
            @PathVariable @Min(value = 1, message = "버전 ID는 1 이상이어야 합니다") Long versionKey,
            @Valid @RequestBody UpdateVersionRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        VersionResponse response = versionService.updateVersion(userKey, versionKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "버전 공개 여부 수정", description = "버전의 공개 여부를 변경합니다 (Public/Private 토글)")
    @PatchMapping("/{versionKey}/visibility")
    public ApiResponse<VersionResponse> updateVisibility(
            @Parameter(description = "버전 ID", required = true)
            @PathVariable @Min(value = 1, message = "버전 ID는 1 이상이어야 합니다") Long versionKey,
            @Valid @RequestBody UpdateVersionVisibilityRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        VersionResponse response = versionService.updateVisibility(userKey, versionKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "버전 Diff 비교 조회", description = "현재 버전과 이전 버전의 diff 비교를 조회합니다")
    @GetMapping("/{versionKey}/compare")
    public ApiResponse<VersionCompareResponse> compareVersion(
            @Parameter(description = "버전 ID", required = true)
            @PathVariable @Min(value = 1, message = "버전 ID는 1 이상이어야 합니다") Long versionKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        VersionCompareResponse response = versionCompareService.compareVersion(userKey, versionKey);
        return ApiResponse.onSuccess(response);
    }
    @PostMapping("/{versionKey}/rollback")
    public ApiResponse<?> rollbackVersion(
            @PathVariable Long versionKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        VersionResponse version = versionService.rollbackToVersion(userKey, versionKey);
        return ApiResponse.onSuccess(version);
    }
}
