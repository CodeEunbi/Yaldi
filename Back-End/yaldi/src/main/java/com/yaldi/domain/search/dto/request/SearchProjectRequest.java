package com.yaldi.domain.search.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "프로젝트 검색 요청")
public record SearchProjectRequest(
        @Schema(description = "검색어", example = "이커머스", required = true)
        @NotBlank(message = "검색어는 필수입니다")
        String query
) {
}
