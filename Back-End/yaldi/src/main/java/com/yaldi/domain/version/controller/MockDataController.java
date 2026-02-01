package com.yaldi.domain.version.controller;

import com.yaldi.domain.version.dto.request.MockDataCreateRequest;
import com.yaldi.domain.version.dto.response.MockDataResponse;
import com.yaldi.domain.version.service.MockDataService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Mock Data", description = "Mock 데이터 생성 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/versions/{versionKey}/mock-data")
@RequiredArgsConstructor
public class MockDataController {

    private final MockDataService mockDataService;

    @Operation( summary = "Mock 데이터 생성 요청", description = "버전의 스키마를 기반으로 Mock 데이터를 생성합니다")
    @PostMapping
    public ApiResponse<MockDataResponse> createMockData(
            @PathVariable Long versionKey,
            @Valid @RequestBody MockDataCreateRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        MockDataResponse response = mockDataService.createMockData(userKey, versionKey, request);

        return ApiResponse.onSuccess(response);
    }

    @Operation( summary = "Version별 Mock 데이터 목록 조회", description = "특정 버전에 생성 완료된 Mock 데이터 목록을 조회합니다")
    @GetMapping
    public ApiResponse<List<MockDataResponse>> getMockDataList(
            @PathVariable Long versionKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        List<MockDataResponse> response = mockDataService.getMockDataList(userKey, versionKey);

        return ApiResponse.onSuccess(response);
    }

    @Operation( summary = "Mock 데이터 조회", description = "Mock 데이터의 생성 상태와 다운로드 URL을 조회합니다. 1시간 유효한 Presigned URL이 반환됩니다")
    @GetMapping("/{mockDataKey}")
    public ApiResponse<MockDataResponse> getMockData(
            @PathVariable Long versionKey,
            @PathVariable Long mockDataKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        MockDataResponse response = mockDataService.getMockData(userKey, versionKey, mockDataKey);

        return ApiResponse.onSuccess(response);
    }
}
