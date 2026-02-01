package com.yaldi.domain.datamodel.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Entity 생성 요청 DTO
 *
 * <p>Entity 생성 흐름:</p>
 * <ul>
 *   <li>1. 테이블 선택</li>
 *   <li>2. [생성] 버튼 클릭</li>
 *   <li>3. 백엔드에서 자동으로 {테이블명PascalCase}Entity 이름 생성</li>
 *   <li>4. 테이블의 모든 컬럼이 자동으로 포함됨</li>
 * </ul>
 *
 * <p>예시: users 테이블 선택 → UsersEntity 자동 생성</p>
 */
@Schema(description = "Entity 생성 요청")
public record CreateEntityRequest(

        @NotNull(message = "테이블 키는 필수입니다")
        @Schema(
                description = "기준 테이블 키",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long tableKey
) {
}
