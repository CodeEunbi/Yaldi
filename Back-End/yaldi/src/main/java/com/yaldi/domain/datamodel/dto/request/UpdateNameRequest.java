package com.yaldi.domain.datamodel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 데이터 모델 이름 수정 요청 DTO
 *
 * <p>상세 페이지에서 이름만 수정</p>
 */
@Schema(description = "데이터 모델 이름 수정 요청")
public record UpdateNameRequest(

        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 1, max = 500, message = "이름은 1자 이상 500자 이하이어야 합니다")
        @Schema(
                description = "새로운 모델 이름",
                example = "UpdatedUserEntity",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name
) {
}
