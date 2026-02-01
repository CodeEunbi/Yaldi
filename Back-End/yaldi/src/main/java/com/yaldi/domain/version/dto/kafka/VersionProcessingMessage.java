package com.yaldi.domain.version.dto.kafka;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
@Schema(description = "버전 검증 Kafka 메시지")
public record VersionProcessingMessage(

        @Schema(description = "작업 ID (AsyncJob ID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String jobId,

        @Schema(description = "버전 Key", example = "10")
        Long versionKey,

        @Schema(description = "프로젝트 Key", example = "5")
        Long projectKey,

        @Schema(description = "프로젝트 이름", example = "E-commerce Project")
        String projectName,

        @Schema(description = "프로젝트 설명")
        String projectDescription,

        @Schema(description = "프로젝트 이미지 URL")
        String projectImageUrl,

        @Schema(description = "버전 이름", example = "v1.0.0")
        String versionName,

        @Schema(description = "버전 설명")
        String versionDescription,

        @Schema(description = "JSONB 스키마 데이터 (테이블 구조)")
        Map<String, Object> schemaData
) {
}
