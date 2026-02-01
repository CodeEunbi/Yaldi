package com.yaldi.domain.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "팀 오너 변경 요청")
public record UpdateTeamOwnerRequest(

        @Schema(description = "새로운 오너의 유저 Key", example = "2")
        @NotNull(message = "양도할 팀원의 유저 Key는 필수입니다")
        Integer newOwnerUserKey
) {
}
