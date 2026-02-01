package com.yaldi.domain.project.dto.response;

import com.yaldi.domain.project.entity.ProjectMemberActionType;
import com.yaldi.domain.project.entity.ProjectMemberHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 프로젝트 멤버 이력 응답 DTO
 */
@Schema(description = "프로젝트 멤버 이력 정보")
public record ProjectMemberHistoryResponse(
    @Schema(description = "이력 ID", example = "1")
    Long projectMemberHistoryKey,

    @Schema(description = "프로젝트 ID", example = "1")
    Long projectKey,

    @Schema(description = "작업 수행자 ID", example = "1")
    Integer actorKey,

    @Schema(description = "작업 수행자 닉네임", example = "admin")
    String actorNickname,

    @Schema(description = "대상 사용자 ID", example = "2")
    Integer targetKey,

    @Schema(description = "대상 사용자 닉네임", example = "user123")
    String targetNickname,

    @Schema(description = "작업 타입", example = "ADD")
    ProjectMemberActionType actionType,

    @Schema(description = "작업 시간")
    OffsetDateTime createdAt
) {
    /**
     * Entity에서 Response DTO로 변환 (닉네임 정보 없음)
     */
    public static ProjectMemberHistoryResponse from(ProjectMemberHistory history) {
        return new ProjectMemberHistoryResponse(
            history.getProjectMemberHistoryKey(),
            history.getProjectKey(),
            history.getActorKey(),
            null,
            history.getTargetKey(),
            null,
            history.getActionType(),
            history.getCreatedAt()
        );
    }

    /**
     * Entity에서 Response DTO로 변환 (닉네임 정보 포함)
     */
    public static ProjectMemberHistoryResponse from(
        ProjectMemberHistory history,
        String actorNickname,
        String targetNickname
    ) {
        return new ProjectMemberHistoryResponse(
            history.getProjectMemberHistoryKey(),
            history.getProjectKey(),
            history.getActorKey(),
            actorNickname,
            history.getTargetKey(),
            targetNickname,
            history.getActionType(),
            history.getCreatedAt()
        );
    }
}
