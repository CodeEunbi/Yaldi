package com.yaldi.domain.team.dto.response;

import com.yaldi.domain.team.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "팀 정보 응답")
public record TeamResponse(

    @Schema(description = "팀 Key", example = "1")
    Integer teamKey,

    @Schema(description = "팀 오너 Key", example = "2")
    Integer ownedBy,

    @Schema(description = "팀 이름", example = "IP팀")
    String name,

    @Schema(description = "생성 시각")
    OffsetDateTime createdAt,

    @Schema(description = "수정 시각")
    OffsetDateTime updatedAt
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getTeamKey(),
                team.getOwner().getUserKey(),
                team.getName(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}