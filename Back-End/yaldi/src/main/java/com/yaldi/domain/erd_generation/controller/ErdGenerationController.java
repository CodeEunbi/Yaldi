package com.yaldi.domain.erd_generation.controller;

import com.yaldi.domain.erd_generation.dto.request.ErdGenerationRequest;
import com.yaldi.domain.erd_generation.dto.response.ErdGenerationResponse;
import com.yaldi.domain.erd_generation.service.ErdGenerationService;
import com.yaldi.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/erd-generation")
@RequiredArgsConstructor
public class ErdGenerationController {

    private final ErdGenerationService erdGenerationService;

    @PostMapping
    public ApiResponse<ErdGenerationResponse> generateErd(
            @Valid @RequestBody ErdGenerationRequest request
    ) {
        log.info("ERD 생성 API 호출 - Project: {}", request.projectName());

        ErdGenerationResponse response = erdGenerationService.generateErd(request);
        return ApiResponse.onSuccess(response);
    }
}
