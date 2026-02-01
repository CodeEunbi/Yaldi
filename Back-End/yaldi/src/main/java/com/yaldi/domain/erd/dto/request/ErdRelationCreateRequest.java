package com.yaldi.domain.erd.dto.request;

import com.yaldi.domain.erd.entity.ReferentialActionType;
import com.yaldi.domain.erd.entity.RelationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ERD 관계 생성 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdRelationCreateRequest {

    @NotNull(message = "시작 테이블 키는 필수입니다")
    @Positive(message = "시작 테이블 키는 양수여야 합니다")
    private Long fromTableKey;

    @Positive(message = "시작 컬럼 키는 양수여야 합니다")
    private Long fromColumnKey;

    @NotNull(message = "대상 테이블 키는 필수입니다")
    @Positive(message = "대상 테이블 키는 양수여야 합니다")
    private Long toTableKey;

    @NotNull(message = "관계 타입은 필수입니다")
    private RelationType relationType;

    @Builder.Default
    @NotBlank(message = "제약 조건명은 필수입니다")
    @Size(max = 64, message = "제약 조건명은 64자 이하여야 합니다")
    private String constraintName = "";

    @Builder.Default
    private ReferentialActionType onDeleteAction = ReferentialActionType.NO_ACTION;

    @Builder.Default
    private ReferentialActionType onUpdateAction = ReferentialActionType.NO_ACTION;
}
