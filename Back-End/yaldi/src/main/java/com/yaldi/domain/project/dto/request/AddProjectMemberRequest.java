package com.yaldi.domain.project.dto.request;

import com.yaldi.domain.project.entity.ProjectMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 프로젝트 멤버 추가 요청 DTO
 */
@Schema(description = "프로젝트 멤버 추가 요청")
public record AddProjectMemberRequest(
    @Schema(description = "추가할 멤버 목록")
    @NotNull(message = "멤버 목록은 필수입니다")
    @NotEmpty(message = "최소 1명 이상의 멤버를 추가해야 합니다")
    @Valid
    List<MemberToAdd> members
) {
    @Schema(description = "추가할 멤버 정보")
    public record MemberToAdd(
        @Schema(description = "추가할 사용자 ID", example = "2")
        @NotNull(message = "사용자 ID는 필수입니다")
        @Min(value = 1, message = "사용자 ID는 1 이상이어야 합니다")
        Integer memberKey,

        @Schema(description = "역할", example = "EDITOR", allowableValues = {"OWNER", "ADMIN", "EDITOR"})
        @NotNull(message = "역할은 필수입니다")
        ProjectMemberRole role
    ) {}
}
