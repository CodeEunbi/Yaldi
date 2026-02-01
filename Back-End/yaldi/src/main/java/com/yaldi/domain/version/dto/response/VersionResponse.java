package com.yaldi.domain.version.dto.response;

import com.yaldi.domain.version.entity.DesignVerificationStatus;
import com.yaldi.domain.version.entity.Version;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "버전 정보")
public record VersionResponse(
    @Schema(description = "버전 ID", example = "1")
    Long versionKey,

    @Schema(description = "프로젝트 ID", example = "15")
    Long projectKey,

    @Schema(description = "버전 이름", example = "v1.0.0")
    String name,

    @Schema(description = "버전 설명", example = "초기 데이터베이스 설계")
    String description,

    @Schema(description = "스키마 데이터 (테이블, 컬럼 정보)")
    Map<String, Object> schemaData,

    @Schema(description = "공개 여부", example = "false")
    Boolean isPublic,

    @Schema(description = "디자인 검증 상태", example = "QUEUED")
    DesignVerificationStatus designVerificationStatus,

    @Schema(description = "검증 에러 목록")
    List<String> verificationErrors,

    @Schema(description = "검증 경고 목록")
    List<String> verificationWarnings,

    @Schema(description = "검증 메시지")
    String verificationMessage,

    @Schema(description = "검증 실패 시 LLM이 생성한 수정 조언")
    List<String> verificationSuggestions,

    @Schema(description = "생성일시")
    OffsetDateTime createdAt,

    @Schema(description = "수정일시")
    OffsetDateTime updatedAt
) {

    public static VersionResponse from(Version version) {
        Map<String, Object> verificationResult = version.getVerificationResult();

        List<String> errors = null;
        List<String> warnings = null;
        String message = null;
        List<String> suggestions = null;

        if (verificationResult != null) {
            errors = (List<String>) verificationResult.get("errors");
            warnings = (List<String>) verificationResult.get("warnings");
            message = (String) verificationResult.get("message");
            suggestions = (List<String>) verificationResult.get("suggestions");
        }

        return new VersionResponse(
            version.getVersionKey(),
            version.getProjectKey(),
            version.getName(),
            version.getDescription(),
            version.getSchemaData(),
            version.getIsPublic(),
            version.getDesignVerificationStatus(),
            errors,
            warnings,
            message,
            suggestions,
            version.getCreatedAt(),
            version.getUpdatedAt()
        );
    }
}
