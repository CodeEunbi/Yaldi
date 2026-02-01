package com.yaldi.domain.version.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Mock 데이터 생성 요청")
public record MockDataCreateRequest(
        @Schema(description = "테이블당 생성할 행 수", example = "100")
        @NotNull(message = "행 수는 필수입니다")
        @Min(value = 1, message = "행 수는 최소 1 이상이어야 합니다")
        @Max(value = 100, message = "행 수는 최대 100 이하여야 합니다")
        Integer rowCount
) {
}