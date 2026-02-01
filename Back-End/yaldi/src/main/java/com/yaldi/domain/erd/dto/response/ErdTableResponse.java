package com.yaldi.domain.erd.dto.response;

import com.yaldi.domain.erd.entity.ErdTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * ERD 테이블 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdTableResponse {

    private Long tableKey;
    private Long projectKey;
    private String logicalName;
    private String physicalName;
    private BigDecimal xPosition;
    private BigDecimal yPosition;
    private String colorHex;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ErdTableResponse from(ErdTable table) {
        return ErdTableResponse.builder()
                .tableKey(table.getTableKey())
                .projectKey(table.getProjectKey())
                .logicalName(table.getLogicalName())
                .physicalName(table.getPhysicalName())
                .xPosition(table.getXPosition())
                .yPosition(table.getYPosition())
                .colorHex(table.getColorHex())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build();
    }
}
