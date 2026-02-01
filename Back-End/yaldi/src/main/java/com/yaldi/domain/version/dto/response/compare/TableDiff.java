package com.yaldi.domain.version.dto.response.compare;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "테이블 차이 정보")
public record TableDiff(
        @Schema(description = "변경 타입 (ADDED, MODIFIED, DELETED, UNCHANGED)")
        ChangeType changeType,

        @Schema(description = "테이블 Key")
        Long tableKey,

        @Schema(description = "물리명")
        String physicalName,

        @Schema(description = "논리명")
        String logicalName,

        @Schema(description = "컬럼 차이 목록")
        List<ColumnDiff> columnDiffs,

        @Schema(description = "변경된 필드 목록 (MODIFIED일 때만)", example = "[\"logicalName\"]")
        List<String> changedFields,

        @Schema(description = "이전 값 (MODIFIED일 때만)")
        Map<String, Object> previousValues
) {
}
