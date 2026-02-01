package com.yaldi.domain.project.dto.response;

import com.yaldi.domain.project.entity.Project;
import com.yaldi.domain.project.entity.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 프로젝트 응답 DTO
 */
@Schema(description = "프로젝트 정보")
public record ProjectResponse(
    @Schema(description = "프로젝트 ID", example = "1")
    Long projectKey,

    @Schema(description = "팀 ID", example = "1")
    Integer teamKey,

    @Schema(description = "프로젝트 이름", example = "이커머스 프로젝트")
    String name,

    @Schema(description = "프로젝트 설명", example = "온라인 쇼핑몰 데이터베이스 설계")
    String description,

    @Schema(description = "프로젝트 이미지 URL")
    String imageUrl,

    @Schema(description = "생성일시")
    OffsetDateTime createdAt,

    @Schema(description = "수정일시")
    OffsetDateTime updatedAt,

    @Schema(description = "마지막 활동 시간")
    OffsetDateTime lastActivityAt,

    @Schema(description = "현재 사용자가 프로젝트 멤버인지 여부", example = "true")
    Boolean isMember,

    @Schema(description = "현재 사용자의 역할 (멤버인 경우에만 포함)", example = "OWNER")
    ProjectMemberRole role
) {

    /**
     * Entity에서 Response DTO로 변환 (역할 정보 포함)
     */
    public static ProjectResponse from(Project project, ProjectMemberRole role) {
        return new ProjectResponse(
            project.getProjectKey(),
            project.getTeamKey(),
            project.getName(),
            project.getDescription(),
            project.getImageUrl(),
            project.getCreatedAt(),
            project.getUpdatedAt(),
            project.getLastActivityAt(),
            role != null,  // isMember
            role
        );
    }
}
