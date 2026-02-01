package com.yaldi.domain.version.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;


@Schema(description = "Mock 데이터 생성 Kafka 메시지")
public record MockDataCreateMessage(

        @Schema(description = "작업 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String jobId,

        @Schema(description = "요청한 사용자 Key", example = "1")
        Integer userKey,

        @Schema(description = "버전 Key", example = "10")
        Long versionKey,

        @Schema(description = "버전 이름", example = "v1.0.0")
        String versionName,

        @Schema(description = "JSONB 스키마 데이터")
        Map<String, Object> schemaData,

        @Schema(description = "생성할 행 수", example = "100")
        Integer rowCount
) {
}
