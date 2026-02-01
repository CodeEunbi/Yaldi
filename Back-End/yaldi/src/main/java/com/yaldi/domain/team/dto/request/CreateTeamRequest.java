package com.yaldi.domain.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "팀 생성 요청")
public record CreateTeamRequest(

        @Schema(description = "팀명", example = "개발팀")
        @NotBlank(message = "팀명은 필수입니다")
        @Size(min = 1, max = 25, message = "팀명은 1자 이상 25자 이하이어야 합니다")
        String name
) {
}
