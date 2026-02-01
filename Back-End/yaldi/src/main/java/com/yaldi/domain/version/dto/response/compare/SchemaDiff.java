package com.yaldi.domain.version.dto.response.compare;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스키마 전체 차이 정보")
public record SchemaDiff(
        @Schema(description = "테이블 차이 목록")
        List<TableDiff> tableDiffs,

        @Schema(description = "관계 차이 목록")
        List<RelationDiff> relationDiffs,

        @Schema(description = "전체 변경 사항 요약")
        DiffSummary summary
) {
}
