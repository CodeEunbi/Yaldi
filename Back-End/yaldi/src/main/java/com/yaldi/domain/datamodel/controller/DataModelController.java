package com.yaldi.domain.datamodel.controller;

import com.yaldi.domain.datamodel.entity.DataModelType;
import com.yaldi.domain.datamodel.dto.request.CreateDtoRequest;
import com.yaldi.domain.datamodel.dto.request.CreateEntityRequest;
import com.yaldi.domain.datamodel.dto.request.UpdateNameRequest;
import com.yaldi.domain.datamodel.dto.response.DataModelDetailResponse;
import com.yaldi.domain.datamodel.dto.response.DataModelResponse;
import com.yaldi.domain.datamodel.service.DataModelService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.PageResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 데이터 모델 관리 API
 *
 * <p>Entity/DTO 자동 생성 및 관리 기능 제공</p>
 */
@Tag(name = "DataModel", description = "데이터 모델 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/projects/{projectKey}/data-models")
@RequiredArgsConstructor
public class DataModelController {

    private final DataModelService dataModelService;

    /**
     * Entity 생성
     *
     * <p>선택한 테이블의 모든 컬럼으로 Entity를 자동 생성합니다.</p>
     */
    @Operation(
            summary = "Entity 생성",
            description = "선택한 테이블의 모든 컬럼으로 Entity를 자동 생성합니다. Entity 이름은 테이블명으로 자동 생성됩니다."
    )
    @PostMapping("/entity")
    public ApiResponse<DataModelResponse> createEntity(
            @Parameter(description = "프로젝트 키") @PathVariable Long projectKey,
            @Valid @RequestBody CreateEntityRequest request) {

        Integer userKey = SecurityUtil.getCurrentUserKey();
        DataModelResponse response = dataModelService.createEntity(userKey, projectKey, request);

        return ApiResponse.onSuccess(response);
    }

    /**
     * DTO 생성
     *
     * <p>여러 테이블의 컬럼을 선택하여 DTO를 생성합니다.</p>
     */
    @Operation(
            summary = "DTO 생성",
            description = "여러 테이블의 컬럼을 선택하여 DTO를 생성합니다. 컬럼명 충돌 시 자동으로 테이블명 prefix가 추가됩니다."
    )
    @PostMapping("/dto")
    public ApiResponse<DataModelResponse> createDto(
            @Parameter(description = "프로젝트 키") @PathVariable Long projectKey,
            @Valid @RequestBody CreateDtoRequest request) {

        Integer userKey = SecurityUtil.getCurrentUserKey();
        DataModelResponse response = dataModelService.createDto(userKey, projectKey, request);

        return ApiResponse.onSuccess(response);
    }

    /**
     * 데이터 모델 목록 조회 (페이지네이션 + 타입 필터링)
     *
     * <p>고정 페이지 크기: 10개, 최신순 정렬</p>
     */
    @Operation(
            summary = "데이터 모델 목록 조회",
            description = "프로젝트의 데이터 모델 목록을 조회합니다. 타입으로 필터링 가능합니다. 페이지 크기는 10개로 고정됩니다."
    )
    @GetMapping
    public ApiResponse<PageResponse<DataModelResponse>> getDataModels(
            @Parameter(description = "프로젝트 키") @PathVariable Long projectKey,
            @Parameter(description = "타입 필터 (ENTITY, DTO_REQUEST, DTO_RESPONSE)") @RequestParam(required = false) DataModelType type,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page) {

        Integer userKey = SecurityUtil.getCurrentUserKey();
        Page<DataModelResponse> dataModelsPage = dataModelService.getDataModels(userKey, projectKey, type, page);

        return ApiResponse.onSuccess(PageResponse.of(dataModelsPage));
    }

    /**
     * 데이터 모델 상세 조회 (생성된 코드 포함)
     */
    @Operation(
            summary = "데이터 모델 상세 조회",
            description = "데이터 모델의 상세 정보와 생성된 코드(Java, TypeScript)를 조회합니다."
    )
    @GetMapping("/{modelKey}")
    public ApiResponse<DataModelDetailResponse> getDataModelDetail(
            @Parameter(description = "프로젝트 키") @PathVariable Long projectKey,
            @Parameter(description = "모델 키") @PathVariable Long modelKey) {

        Integer userKey = SecurityUtil.getCurrentUserKey();
        DataModelDetailResponse response = dataModelService.getDataModelDetail(userKey, projectKey, modelKey);

        return ApiResponse.onSuccess(response);
    }

    /**
     * 데이터 모델 Refresh (동기화)
     *
     * <p>ERD 변경사항을 반영하고 동기화 상태를 업데이트합니다.</p>
     */
    @Operation(
            summary = "데이터 모델 Refresh",
            description = "ERD 변경사항을 반영하고 last_synced_at를 현재 시각으로 업데이트합니다. INVALID 상태인 경우 Refresh 불가능합니다."
    )
    @PostMapping("/{modelKey}/refresh")
    public ApiResponse<DataModelResponse> refreshDataModel(
            @Parameter(description = "프로젝트 키") @PathVariable Long projectKey,
            @Parameter(description = "모델 키") @PathVariable Long modelKey) {

        Integer userKey = SecurityUtil.getCurrentUserKey();
        DataModelResponse response = dataModelService.refreshDataModel(userKey, projectKey, modelKey);

        return ApiResponse.onSuccess(response);
    }

    /**
     * 데이터 모델 이름 수정
     */
    @Operation(
            summary = "데이터 모델 이름 수정",
            description = "데이터 모델의 이름을 수정합니다."
    )
    @PatchMapping("/{modelKey}/name")
    public ApiResponse<DataModelResponse> updateName(
            @Parameter(description = "프로젝트 키") @PathVariable Long projectKey,
            @Parameter(description = "모델 키") @PathVariable Long modelKey,
            @Valid @RequestBody UpdateNameRequest request) {

        Integer userKey = SecurityUtil.getCurrentUserKey();
        DataModelResponse response = dataModelService.updateName(userKey, projectKey, modelKey, request);

        return ApiResponse.onSuccess(response);
    }

    /**
     * 데이터 모델 삭제
     */
    @Operation(
            summary = "데이터 모델 삭제",
            description = "데이터 모델을 삭제합니다 (Soft Delete)."
    )
    @DeleteMapping("/{modelKey}")
    public ApiResponse<?> deleteDataModel(
            @Parameter(description = "프로젝트 키") @PathVariable Long projectKey,
            @Parameter(description = "모델 키") @PathVariable Long modelKey) {

        Integer userKey = SecurityUtil.getCurrentUserKey();
        dataModelService.deleteDataModel(userKey, projectKey, modelKey);

        return ApiResponse.OK;
    }
}
