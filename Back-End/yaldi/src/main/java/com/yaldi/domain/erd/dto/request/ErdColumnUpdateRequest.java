package com.yaldi.domain.erd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ERD 컬럼 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdColumnUpdateRequest {

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
}
