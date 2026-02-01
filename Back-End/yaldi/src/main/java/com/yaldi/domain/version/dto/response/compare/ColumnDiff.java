package com.yaldi.domain.version.dto.response.compare;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "컬럼 차이 정보")
public record ColumnDiff(
        @Schema(description = "변경 타입 (ADDED, MODIFIED, DELETED, UNCHANGED)")
        ChangeType changeType,

        @Schema(description = "컬럼 Key")
        Long columnKey,

        @Schema(description = "물리명")
        String physicalName,

        @Schema(description = "논리명")
        String logicalName,

        @Schema(description = "데이터 타입")
        String dataType,

        @Schema(description = "데이터 상세 (길이, 정밀도 등)")
        List<Object> dataDetail,

        @Schema(description = "Primary Key 여부")
        Boolean isPrimaryKey,

        @Schema(description = "Nullable 여부")
        Boolean isNullable,

        @Schema(description = "Unique 여부")
        Boolean isUnique,

        @Schema(description = "Foreign Key 여부")
        Boolean isForeignKey,

        @Schema(description = "자동 증가 여부")
        Boolean isIncremental,

        @Schema(description = "기본값")
        String defaultValue,

        @Schema(description = "변경된 필드 목록 (MODIFIED일 때만)", example = "[\"dataType\", \"isNullable\"]")
        List<String> changedFields,

        @Schema(description = "이전 값 (MODIFIED일 때만)")
        Map<String, Object> previousValues
) {
}
