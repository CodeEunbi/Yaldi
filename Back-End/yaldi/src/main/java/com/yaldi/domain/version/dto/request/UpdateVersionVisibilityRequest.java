package com.yaldi.domain.version.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 버전 공개 여부 수정 요청 DTO
 */
@Schema(description = "버전 공개 여부 수정 요청")
public record UpdateVersionVisibilityRequest(
    @Schema(description = "공개 여부 (true: Public, false: Private)", example = "true")
    @NotNull(message = "공개 여부는 필수입니다")
    Boolean isPublic
) {
}
