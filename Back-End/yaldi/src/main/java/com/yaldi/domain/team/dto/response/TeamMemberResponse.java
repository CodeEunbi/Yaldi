package com.yaldi.domain.team.dto.response;

import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.entity.UserTeamRelation;

import com.yaldi.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀 멤버 응답")
public record TeamMemberResponse(

        @Schema(description = "유저 Key", example = "2")
        Integer userKey,

        @Schema(description = "유저 닉네임", example = "지현")
        String nickName,

        @Schema(description = "팀 오너 여부", example = "false")
        Boolean isOwner

) {

    public static TeamMemberResponse from(UserTeamRelation userTeamRelation, Team team, User user) {
        return new TeamMemberResponse(
                userTeamRelation.getUser().getUserKey(),
                user.getNickname(),
                team.getOwner().getUserKey().equals(userTeamRelation.getUser().getUserKey())
        );
    }
}
