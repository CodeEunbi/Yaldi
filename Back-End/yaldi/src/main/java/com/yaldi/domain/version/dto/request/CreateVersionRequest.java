package com.yaldi.domain.version.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 버전 생성 요청 DTO
 */
@Schema(description = "버전 생성 요청")
public record CreateVersionRequest(
    @Schema(description = "버전 이름", example = "v1.0.0")
    @NotBlank(message = "버전 이름은 필수입니다")
    @Size(max = 255, message = "버전 이름은 최대 255자까지 입력 가능합니다")
    String name,

    @Schema(description = "버전 설명", example = "초기 데이터베이스 설계")
    @Size(max = 1000, message = "버전 설명은 최대 1000자까지 입력 가능합니다")
    String description,

    @Schema(description = "스키마 데이터 (테이블, 컬럼 정보)")
    @NotNull(message = "스키마 데이터는 필수입니다")
    Map<String, Object> schemaData,

    @Schema(description = "공개 여부", example = "false")
    Boolean isPublic
) {
}
