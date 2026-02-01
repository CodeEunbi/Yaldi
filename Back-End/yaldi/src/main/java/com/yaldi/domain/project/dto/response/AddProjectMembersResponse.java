package com.yaldi.domain.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 프로젝트 멤버 추가 응답 DTO (부분 성공 지원)
 */
@Schema(description = "프로젝트 멤버 추가 응답")
public record AddProjectMembersResponse(
    @Schema(description = "성공적으로 추가된 멤버 목록")
    List<ProjectMemberResponse> succeeded,

    @Schema(description = "추가 실패한 멤버 목록")
    List<MemberAddFailure> failed
) {
    @Schema(description = "멤버 추가 실패 정보")
    public record MemberAddFailure(
        @Schema(description = "실패한 사용자 ID", example = "2")
        Integer memberKey,

        @Schema(description = "에러 코드", example = "USER4000")
        String errorCode,

        @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
        String errorMessage
    ) {}
}
