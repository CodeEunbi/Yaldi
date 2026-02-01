package com.yaldi.domain.project.dto.response;

import com.yaldi.domain.project.entity.ProjectMemberRelation;
import com.yaldi.domain.project.entity.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * 프로젝트 멤버 응답 DTO
 */
@Schema(description = "프로젝트 멤버 정보")
public record ProjectMemberResponse(
    @Schema(description = "멤버 관계 ID", example = "1")
    Long projectMemberRelationKey,

    @Schema(description = "사용자 ID", example = "1")
    Integer memberKey,

    @Schema(description = "사용자 닉네임", example = "user123")
    String nickname,

    @Schema(description = "사용자 이메일", example = "user@example.com")
    String email,

    @Schema(description = "역할", example = "OWNER")
    ProjectMemberRole role,

    @Schema(description = "추가일시")
    OffsetDateTime createdAt
) {
    /**
     * Entity에서 Response DTO로 변환 (사용자 정보 없음)
     */
    public static ProjectMemberResponse from(ProjectMemberRelation relation) {
        return new ProjectMemberResponse(
            relation.getProjectMemberRelationKey(),
            relation.getMemberKey(),
            null,
            null,
            relation.getRole(),
            relation.getCreatedAt()
        );
    }

    /**
     * Entity에서 Response DTO로 변환 (사용자 정보 포함)
     */
    public static ProjectMemberResponse from(ProjectMemberRelation relation, String nickname, String email) {
        return new ProjectMemberResponse(
            relation.getProjectMemberRelationKey(),
            relation.getMemberKey(),
            nickname,
            email,
            relation.getRole(),
            relation.getCreatedAt()
        );
    }
}
