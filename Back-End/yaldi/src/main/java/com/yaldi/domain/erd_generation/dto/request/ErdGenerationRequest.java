package com.yaldi.domain.erd_generation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ErdGenerationRequest(
        @NotBlank(message = "프로젝트명은 필수입니다")
        @Size(min = 1, max = 200, message = "프로젝트명은 1~200자 사이여야 합니다")
        String projectName,

        @Size(max = 1000, message = "프로젝트 설명은 최대 1000자입니다")
        String projectDescription,

        @NotBlank(message = "AI 초안 요청 내용은 필수입니다")
        @Size(min = 10, max = 2000, message = "AI 초안 요청은 10~2000자 사이여야 합니다")
        String userPrompt
) {
}
