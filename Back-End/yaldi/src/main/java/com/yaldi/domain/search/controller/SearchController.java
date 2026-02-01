package com.yaldi.domain.search.controller;


import com.yaldi.domain.search.dto.request.SearchProjectRequest;
import com.yaldi.domain.search.dto.response.ProjectSearchResponse;
import com.yaldi.domain.search.service.VersionSearchService;
import com.yaldi.domain.version.dto.response.VersionListResponse;
import com.yaldi.domain.version.dto.response.VersionResponse;
import com.yaldi.domain.version.service.VersionService;
import com.yaldi.global.response.ApiResponse;
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

import java.util.List;

@Tag(name="Search", description = "검색 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final VersionSearchService versionSearchService;
    private final VersionService versionService;

    @Operation( summary = "프로젝트 검색", description = "Public 버전들을 검색(텍스트 + 벡터)하여 프로젝트 목록 반환")
    @GetMapping("/projects")
    public ApiResponse<List<ProjectSearchResponse>> searchProjects(
            @ModelAttribute @Valid SearchProjectRequest request
    ) {
        log.info("프로젝트 검색 요청 - Query: {}", request.query());

        List<ProjectSearchResponse> results = versionSearchService.searchProjects(request.query());
        return ApiResponse.onSuccess(results);
    }

    @Operation(summary = "프로젝트의 Public 버전 리스트 조회", description = "검색 결과에서 프로젝트 선택 시 해당 프로젝트의 모든 Public 버전 리스트를 최신순으로 반환 (권한 확인 없음)")
    @GetMapping("/projects/{projectKey}/versions")
    public ApiResponse<List<VersionListResponse>> getPublicVersionsByProject(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        log.info("프로젝트 Public 버전 리스트 조회 요청 - ProjectKey: {}", projectKey);

        List<VersionListResponse> results = versionService.getPublicVersions(projectKey);
        return ApiResponse.onSuccess(results);
    }

    @Operation(summary = "Public 버전 상세 조회 (ERD)", description = "검색 결과에서 버전 선택 시 해당 버전의 ERD 정보(schemaData)를 조회합니다 (권한 확인 없음)")
    @GetMapping("/projects/{projectKey}/versions/{versionKey}")
    public ApiResponse<VersionResponse> getPublicVersion(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Parameter(description = "버전 ID", required = true)
            @PathVariable @Min(value = 1, message = "버전 ID는 1 이상이어야 합니다") Long versionKey
    ) {
        log.info("Public 버전 상세 조회 요청 - ProjectKey: {}, VersionKey: {}", projectKey, versionKey);

        VersionResponse result = versionService.getPublicVersion(projectKey, versionKey);
        return ApiResponse.onSuccess(result);
    }
}
