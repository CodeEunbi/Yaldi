package com.yaldi.domain.erd.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * ErdRelation 엔티티
 */
@Entity
@Table(name = "erd_relations")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdRelation extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relation_key")
    private Long relationKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "from_table_key", nullable = false)
    private Long fromTableKey;

    @Column(name = "from_column_key")
    private Long fromColumnKey;

    @Column(name = "to_table_key", nullable = false)
    private Long toTableKey;

    @Column(name = "to_column_key")
    private Long toColumnKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "relation_type", nullable = false)
    private RelationType relationType;

    @Column(name = "constraint_name", length = 255)
    @Builder.Default
    private String constraintName = "";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "on_delete_action", nullable = false)
    @Builder.Default
    private ReferentialActionType onDeleteAction = ReferentialActionType.NO_ACTION;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "on_update_action", nullable = false)
    @Builder.Default
    private ReferentialActionType onUpdateAction = ReferentialActionType.NO_ACTION;

    // 비즈니스 로직
    public void updateRelationType(RelationType relationType) {
        if (relationType == null) {
            throw new IllegalArgumentException("Relation type cannot be null");
        }
        this.relationType = relationType;
    }

    public void updateConstraintName(String constraintName) {
        if (constraintName == null) {
            this.constraintName = "";
        } else {
            this.constraintName = constraintName;
        }
    }

    public void updateReferentialActions(ReferentialActionType onDeleteAction, ReferentialActionType onUpdateAction) {
        if (onDeleteAction == null || onUpdateAction == null) {
            throw new IllegalArgumentException("Referential actions cannot be null");
        }
        this.onDeleteAction = onDeleteAction;
        this.onUpdateAction = onUpdateAction;
    }

    public void updateColumns(Long fromColumnKey, Long toColumnKey) {
        this.fromColumnKey = fromColumnKey;
        this.toColumnKey = toColumnKey;
    }
}
