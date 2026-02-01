package com.yaldi.domain.version.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "버전 수정 요청")
public record UpdateVersionRequest(
    @Schema(description = "버전 이름", example = "v1.0.1")
    @Size(max = 255, message = "버전 이름은 최대 255자까지 입력 가능합니다")
    String name,

    @Schema(description = "버전 설명", example = "수정된 설명")
    @Size(max = 1000, message = "버전 설명은 최대 1000자까지 입력 가능합니다")
    String description
) {
}
