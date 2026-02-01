package com.yaldi.domain.edithistory.controller;

import com.yaldi.domain.edithistory.dto.response.EditHistoryResponse;
import com.yaldi.domain.edithistory.entity.EditHistoryTargetType;
import com.yaldi.domain.edithistory.service.EditHistoryService;
import com.yaldi.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "EditHistory", description = "편집 히스토리 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/edit-history")
@RequiredArgsConstructor
@Validated
public class EditHistoryController {

    private final EditHistoryService editHistoryService;

    @Operation(summary = "프로젝트 편집 히스토리 조회", description = "특정 프로젝트의 편집 히스토리를 조회합니다 (최신순)")
    @GetMapping("/projects/{projectKey}")
    public ResponseEntity<ApiResponse<List<EditHistoryResponse>>> getProjectEditHistory(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        List<EditHistoryResponse> response = editHistoryService.getHistoryByProject(projectKey)
                .stream()
                .map(EditHistoryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "프로젝트의 특정 타입 편집 히스토리 조회", description = "특정 프로젝트의 특정 타입(TABLE/COLUMN/RELATION)의 편집 히스토리를 조회합니다")
    @GetMapping("/projects/{projectKey}/types/{targetType}")
    public ResponseEntity<ApiResponse<List<EditHistoryResponse>>> getProjectEditHistoryByType(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey,
            @Parameter(description = "타겟 타입 (TABLE/COLUMN/RELATION)", required = true)
            @PathVariable EditHistoryTargetType targetType
    ) {
        List<EditHistoryResponse> response = editHistoryService.getHistoryByProjectAndType(projectKey, targetType)
                .stream()
                .map(EditHistoryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "특정 타겟의 편집 히스토리 조회", description = "특정 테이블/컬럼/관계의 편집 히스토리를 조회합니다")
    @GetMapping("/targets/{targetKey}")
    public ResponseEntity<ApiResponse<List<EditHistoryResponse>>> getTargetEditHistory(
            @Parameter(description = "타겟 ID (table_key, column_key, relation_key)", required = true)
            @PathVariable Long targetKey
    ) {
        List<EditHistoryResponse> response = editHistoryService.getHistoryByTarget(targetKey)
                .stream()
                .map(EditHistoryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "사용자 편집 히스토리 조회", description = "특정 사용자의 편집 히스토리를 조회합니다 (최신순)")
    @GetMapping("/users/{userKey}")
    public ResponseEntity<ApiResponse<List<EditHistoryResponse>>> getUserEditHistory(
            @Parameter(description = "사용자 ID", required = true)
            @PathVariable Integer userKey
    ) {
        List<EditHistoryResponse> response = editHistoryService.getHistoryByUser(userKey)
                .stream()
                .map(EditHistoryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
