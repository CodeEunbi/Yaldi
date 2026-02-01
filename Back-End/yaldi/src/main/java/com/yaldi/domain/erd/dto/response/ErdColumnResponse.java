package com.yaldi.domain.erd.dto.response;

import com.yaldi.domain.erd.entity.ErdColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * ERD 컬럼 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdColumnResponse {

    private Long columnKey;
    private Long tableKey;
    private String logicalName;
    private String physicalName;
    private String dataType;
    private String[] dataDetail;
    private Boolean isNullable;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private Boolean isUnique;
    private Boolean isIncremental;
    private String defaultValue;
    private String comment;
    private Integer columnOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ErdColumnResponse from(ErdColumn column) {
        return ErdColumnResponse.builder()
                .columnKey(column.getColumnKey())
                .tableKey(column.getTableKey())
                .logicalName(column.getLogicalName())
                .physicalName(column.getPhysicalName())
                .dataType(column.getDataType())
                .dataDetail(column.getDataDetail())
                .isNullable(column.getIsNullable())
                .isPrimaryKey(column.getIsPrimaryKey())
                .isForeignKey(column.getIsForeignKey())
                .isUnique(column.getIsUnique())
                .isIncremental(column.getIsIncremental())
                .defaultValue(column.getDefaultValue())
                .comment(column.getComment())
                .columnOrder(column.getColumnOrder())
                .createdAt(column.getCreatedAt())
                .updatedAt(column.getUpdatedAt())
                .build();
    }
}
