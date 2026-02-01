package com.yaldi.domain.version.controller;

import com.yaldi.domain.version.dto.request.CreateVersionRequest;
import com.yaldi.domain.version.dto.response.VersionListResponse;
import com.yaldi.domain.version.dto.response.VersionResponse;
import com.yaldi.domain.version.service.VersionService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.PageResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Version", description = "버전 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/projects/{projectKey}/versions")
@RequiredArgsConstructor
@Validated
public class ProjectVersionController {

    private final VersionService versionService;

    @Operation(summary = "버전 생성", description = "프로젝트에 새로운 버전을 생성합니다")
    @PostMapping
    public ApiResponse<VersionResponse> createVersion(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Valid @RequestBody CreateVersionRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        VersionResponse response = versionService.createVersion(userKey, projectKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "버전 목록 조회", description = "프로젝트의 모든 버전을 조회합니다 (간단한 정보만, version_key 기준 최신순, 페이지당 10개)")
    @GetMapping
    public ApiResponse<PageResponse<VersionListResponse>> getVersions(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Parameter(description = "페이지 번호 (0부터 시작)", required = false)
            @RequestParam(defaultValue = "0") int page
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Page<VersionListResponse> versionsPage = versionService.getVersions(userKey, projectKey, page);
        return ApiResponse.onSuccess(PageResponse.of(versionsPage));
    }
}
