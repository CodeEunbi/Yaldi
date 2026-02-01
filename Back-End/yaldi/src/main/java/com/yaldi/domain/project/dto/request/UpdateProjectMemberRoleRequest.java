package com.yaldi.domain.project.dto.request;

import com.yaldi.domain.project.entity.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 프로젝트 멤버 역할 변경 요청 DTO
 */
@Schema(description = "프로젝트 멤버 역할 변경 요청")
public record UpdateProjectMemberRoleRequest(
    @Schema(description = "새로운 역할", example = "ADMIN", allowableValues = {"OWNER", "ADMIN", "EDITOR"})
    @NotNull(message = "역할은 필수입니다")
    ProjectMemberRole role
) {
}
