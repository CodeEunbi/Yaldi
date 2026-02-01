package com.yaldi.domain.erd.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * ErdColumn 엔티티
 */
@Entity
@Table(name = "erd_columns")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdColumn extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "column_key")
    private Long columnKey;

    @Column(name = "table_key", nullable = false)
    private Long tableKey;

    @Column(name = "logical_name", length = 255)
    private String logicalName;

    @Column(name = "physical_name", length = 255)
    private String physicalName;

    @Column(name = "data_type", length = 255)
    private String dataType;

    @Column(name = "data_detail", columnDefinition = "TEXT[]")
    private String[] dataDetail;

    @Column(name = "is_nullable", nullable = false)
    @Builder.Default
    private Boolean isNullable = true;

    @Column(name = "is_primary_key", nullable = false)
    @Builder.Default
    private Boolean isPrimaryKey = false;

    @Column(name = "is_foreign_key", nullable = false)
    @Builder.Default
    private Boolean isForeignKey = false;

    @Column(name = "is_unique", nullable = false)
    @Builder.Default
    private Boolean isUnique = false;

    @Column(name = "is_incremental", nullable = false)
    @Builder.Default
    private Boolean isIncremental = false;

    @Column(name = "default_value", length = 255)
    private String defaultValue;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "column_order", nullable = false)
    @Builder.Default
    private Integer columnOrder = 0;

    // 비즈니스 로직
    public void updateLogicalName(String logicalName) {
        this.logicalName = logicalName;
    }

    public void updatePhysicalName(String physicalName) {
        this.physicalName = physicalName;
    }

    public void updateDataType(String dataType, String[] dataDetail) {
        this.dataType = dataType;
        this.dataDetail = dataDetail;
    }

    public void updateConstraints(Boolean isNullable, Boolean isPrimaryKey, Boolean isForeignKey, Boolean isUnique, Boolean isIncremental) {
        this.isNullable = isNullable;
        this.isPrimaryKey = isPrimaryKey;
        this.isForeignKey = isForeignKey;
        this.isUnique = isUnique;
        this.isIncremental = isIncremental;
    }

    public void updateDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void updateComment(String comment) {
        this.comment = comment;
    }

    public void updateColumnOrder(Integer columnOrder) {
        if (columnOrder == null || columnOrder < 0) {
            throw new IllegalArgumentException("Column order must be non-negative");
        }
        this.columnOrder = columnOrder;
    }
}
