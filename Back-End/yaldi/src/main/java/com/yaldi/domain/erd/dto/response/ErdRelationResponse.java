package com.yaldi.domain.erd.dto.response;

import com.yaldi.domain.erd.entity.ReferentialActionType;
import com.yaldi.domain.erd.entity.RelationType;
import com.yaldi.domain.erd.entity.ErdRelation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ERD 관계 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdRelationResponse {

    private Long relationKey;
    private Long projectKey;
    private Long fromTableKey;
    private Long fromColumnKey;
    private Long toTableKey;
    private Long toColumnKey;
    private RelationType relationType;
    private String constraintName;
    private ReferentialActionType onDeleteAction;
    private ReferentialActionType onUpdateAction;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ErdRelationResponse from(ErdRelation relation) {
        return ErdRelationResponse.builder()
                .relationKey(relation.getRelationKey())
                .projectKey(relation.getProjectKey())
                .fromTableKey(relation.getFromTableKey())
                .fromColumnKey(relation.getFromColumnKey())
                .toTableKey(relation.getToTableKey())
                .toColumnKey(relation.getToColumnKey())
                .relationType(relation.getRelationType())
                .constraintName(relation.getConstraintName())
                .onDeleteAction(relation.getOnDeleteAction())
                .onUpdateAction(relation.getOnUpdateAction())
                .createdAt(relation.getCreatedAt())
                .updatedAt(relation.getUpdatedAt())
                .build();
    }
}
