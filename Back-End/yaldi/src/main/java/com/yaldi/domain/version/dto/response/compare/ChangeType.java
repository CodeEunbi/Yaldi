package com.yaldi.domain.version.dto.response.compare;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "변경 타입")
public enum ChangeType {
    @Schema(description = "새로 추가됨 (파란색)")
    ADDED,

    @Schema(description = "수정됨 (빨간색)")
    MODIFIED,

    @Schema(description = "삭제됨 (회색)")
    DELETED,

    @Schema(description = "변경 없음")
    UNCHANGED
}
