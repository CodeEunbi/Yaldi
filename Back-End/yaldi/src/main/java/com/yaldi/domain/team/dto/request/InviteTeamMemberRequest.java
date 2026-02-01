package com.yaldi.domain.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "팀 멤버 초대 요청")
public record InviteTeamMemberRequest(

        @Schema(description = "초대할 사용자 Key", example = "5")
        @NotNull(message = "초대할 사용자를 선택해주세요.")
        Integer targetUserKey
) {
}
