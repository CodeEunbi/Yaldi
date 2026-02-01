package com.yaldi.domain.version.dto.response.compare;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "테이블 관계 차이 정보")
public record RelationDiff(
        @Schema(description = "변경 타입 (ADDED, MODIFIED, DELETED, UNCHANGED)")
        ChangeType changeType,

        @Schema(description = "시작 테이블 Key")
        Long fromTableKey,

        @Schema(description = "대상 테이블 Key")
        Long toTableKey,

        @Schema(description = "관계 타입")
        String relationType,

        @Schema(description = "제약조건 이름")
        String constraintName,

        @Schema(description = "ON DELETE 액션")
        String onDeleteAction,

        @Schema(description = "ON UPDATE 액션")
        String onUpdateAction,

        @Schema(description = "변경된 필드 목록 (MODIFIED일 때만)", example = "[\"onDeleteAction\", \"relationType\"]")
        List<String> changedFields,

        @Schema(description = "이전 값 (MODIFIED일 때만)")
        Map<String, Object> previousValues
) {
}
