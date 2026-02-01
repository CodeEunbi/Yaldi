package com.yaldi.domain.consultation.controller;

import com.yaldi.domain.consultation.dto.response.ConsultationHistoryResponse;
import com.yaldi.domain.consultation.dto.response.ConsultationMessageResponse;
import com.yaldi.domain.consultation.dto.request.SendConsultationRequest;
import com.yaldi.domain.consultation.service.ConsultationService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Consultation", description = "ERD 상담 챗봇 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/consultation")
@RequiredArgsConstructor
@Validated
public class ConsultationController {

    private final ConsultationService consultationService;

    @Operation(summary = "상담 메시지 전송", description = "ERD 설계 관련 질문을 AI에게 전송하고 답변을 받습니다")
    @PostMapping
    public ApiResponse<ConsultationMessageResponse> sendMessage(
            @Valid @RequestBody SendConsultationRequest request
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        ConsultationMessageResponse response = consultationService.sendMessage(userKey, request);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "상담 대화 내역 조회", description = "특정 프로젝트의 전체 대화 내역을 시간순으로 조회합니다")
    @GetMapping("/projects/{projectKey}/history")
    public ApiResponse<ConsultationHistoryResponse> getHistory(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        ConsultationHistoryResponse response = consultationService.getHistory(userKey, projectKey);
        return ApiResponse.onSuccess(response);
    }
}
