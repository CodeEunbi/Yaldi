package com.yaldi.domain.datamodel.dto.request;

import com.yaldi.domain.datamodel.entity.DataModelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO 생성 요청 DTO
 *
 * <p>DTO 생성 흐름:</p>
 * <ul>
 *   <li>1. 여러 테이블의 컬럼 선택 (다중 선택 가능)</li>
 *   <li>2. DTO 이름 입력 (필수)</li>
 *   <li>3. DTO 타입 선택 (DTO_REQUEST / DTO_RESPONSE)</li>
 *   <li>4. 백엔드에서 컬럼명 충돌 자동 해결</li>
 * </ul>
 */
@Schema(description = "DTO 생성 요청")
public record CreateDtoRequest(

        @NotBlank(message = "DTO명은 필수입니다")
        @Size(min = 1, max = 500, message = "DTO명은 1자 이상 500자 이하이어야 합니다")
        @Schema(
                description = "DTO 이름",
                example = "UserOrderResponse",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @NotNull(message = "DTO 타입은 필수입니다")
        @Schema(
                description = "DTO 타입",
                example = "DTO_RESPONSE",
                allowableValues = {"DTO_REQUEST", "DTO_RESPONSE"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        DataModelType type,

        @NotEmpty(message = "최소 1개 이상의 컬럼을 선택해야 합니다")
        @Valid
        @Schema(
                description = "선택된 컬럼 목록",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<SelectedColumnDto> selectedColumns
) {
}
