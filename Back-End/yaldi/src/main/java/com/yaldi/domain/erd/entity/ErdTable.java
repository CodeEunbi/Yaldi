package com.yaldi.domain.erd.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * ErdTable 엔티티
 */
@Entity
@Table(name = "erd_tables")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdTable extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_key")
    private Long tableKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "logical_name", length = 255, nullable = false)
    @Builder.Default
    private String logicalName = "";

    @Column(name = "physical_name", length = 255, nullable = false)
    @Builder.Default
    private String physicalName = "";

    @Column(name = "x_position", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal xPosition = BigDecimal.ZERO;

    @Column(name = "y_position", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal yPosition = BigDecimal.ZERO;

    @Column(name = "color_hex", length = 6)
    private String colorHex;

    // 비즈니스 로직
    public void updateLogicalName(String logicalName) {
        if (logicalName == null || logicalName.trim().isEmpty()) {
            throw new IllegalArgumentException("Logical name cannot be empty");
        }
        this.logicalName = logicalName;
    }

    public void updatePhysicalName(String physicalName) {
        if (physicalName == null || physicalName.trim().isEmpty()) {
            throw new IllegalArgumentException("Physical name cannot be empty");
        }
        this.physicalName = physicalName;
    }

    public void updatePosition(BigDecimal xPosition, BigDecimal yPosition) {
        if (xPosition == null || yPosition == null) {
            throw new IllegalArgumentException("Position values cannot be null");
        }
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }

    public void updateColorHex(String colorHex) {
        if (colorHex != null && !colorHex.matches("^[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("Invalid color hex format");
        }
        this.colorHex = colorHex;
    }
}
