package com.yaldi.domain.datamodel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO 생성 시 선택된 컬럼 정보
 *
 * <p>컬럼명 충돌 처리:</p>
 * <ul>
 *   <li>백엔드에서 자동으로 감지 및 해결</li>
 *   <li>중복되면: 테이블명 prefix 자동 추가 (users.id → userId, orders.id → orderId)</li>
 * </ul>
 */
@Schema(description = "선택된 컬럼 정보")
public record SelectedColumnDto(

        @NotNull(message = "컬럼 키는 필수입니다")
        @Schema(
                description = "컬럼 키",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long columnKey,

        @NotNull(message = "테이블 키는 필수입니다")
        @Schema(
                description = "테이블 키",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long tableKey
) {
}
