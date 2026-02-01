package com.yaldi.domain.erd.dto.request;

import com.yaldi.domain.erd.entity.ReferentialActionType;
import com.yaldi.domain.erd.entity.RelationType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ERD 관계 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdRelationUpdateRequest {

    @Positive(message = "시작 컬럼 키는 양수여야 합니다")
    private Long fromColumnKey;

    @Positive(message = "대상 컬럼 키는 양수여야 합니다")
    private Long toColumnKey;

    private RelationType relationType;

    @Size(max = 64, message = "제약 조건명은 64자 이하여야 합니다")
    private String constraintName;

    private ReferentialActionType onDeleteAction;

    private ReferentialActionType onUpdateAction;
}
