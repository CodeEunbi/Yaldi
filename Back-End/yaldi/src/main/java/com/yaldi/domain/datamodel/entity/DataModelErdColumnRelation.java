package com.yaldi.domain.datamodel.entity;

import com.yaldi.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DataModelErdColumnRelation 엔티티
 */
@Entity
@Table(
    name = "data_model_erd_column_relations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_column_model",
            columnNames = {"column_key", "model_key"}
        )
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataModelErdColumnRelation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_column_relation_key")
    private Long modelColumnRelationKey;

    @Column(name = "column_key", nullable = false)
    private Long columnKey;

    @Column(name = "model_key", nullable = false)
    private Long modelKey;
}
