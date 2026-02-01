package com.yaldi.domain.version.dto.response;

import com.yaldi.domain.version.entity.DesignVerificationStatus;
import com.yaldi.domain.version.entity.Version;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "버전 목록 정보")
public record VersionListResponse(
    @Schema(description = "버전 ID", example = "1")
    Long versionKey,

    @Schema(description = "프로젝트 ID", example = "15")
    Long projectKey,

    @Schema(description = "버전 이름", example = "v1.0.0")
    String name,

    @Schema(description = "버전 설명", example = "초기 데이터베이스 설계")
    String description,

    @Schema(description = "공개 여부", example = "false")
    Boolean isPublic,

    @Schema(description = "디자인 검증 상태", example = "QUEUED")
    DesignVerificationStatus designVerificationStatus,

    @Schema(description = "생성일시")
    OffsetDateTime createdAt,

    @Schema(description = "수정일시")
    OffsetDateTime updatedAt
) {

    public static VersionListResponse from(Version version) {
        return new VersionListResponse(
            version.getVersionKey(),
            version.getProjectKey(),
            version.getName(),
            version.getDescription(),
            version.getIsPublic(),
            version.getDesignVerificationStatus(),
            version.getCreatedAt(),
            version.getUpdatedAt()
        );
    }
}
