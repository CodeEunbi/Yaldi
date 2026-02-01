package com.yaldi.domain.version.dto.response.compare;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "변경 사항 요약")
public record DiffSummary(
        @Schema(description = "추가된 테이블 수")
        int addedTables,

        @Schema(description = "수정된 테이블 수")
        int modifiedTables,

        @Schema(description = "삭제된 테이블 수")
        int deletedTables,

        @Schema(description = "추가된 컬럼 수")
        int addedColumns,

        @Schema(description = "수정된 컬럼 수")
        int modifiedColumns,

        @Schema(description = "삭제된 컬럼 수")
        int deletedColumns,

        @Schema(description = "추가된 관계 수")
        int addedRelations,

        @Schema(description = "수정된 관계 수")
        int modifiedRelations,

        @Schema(description = "삭제된 관계 수")
        int deletedRelations,

        @Schema(description = "변경 사항이 있는지 여부")
        boolean hasChanges
) {
}
