package com.yaldi.domain.team.dto.response;

import com.yaldi.domain.team.entity.UserTeamActionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "초대 수락/거절 응답")
public record InvitationActionResponse(

        @Schema(description = "초대 Key", example = "1")
        Long invitationKey,

        @Schema(description = "팀 Key", example = "5")
        Integer teamKey,

        @Schema(description = "액션 결과", example = "INVITE_ACCEPTED")
        UserTeamActionType action,

        @Schema(description = "메시지", example = "팀 초대를 수락했습니다.")
        String message
) {

    public static InvitationActionResponse accepted(Long invitationKey, Integer teamKey) {
        return new InvitationActionResponse(
                invitationKey,
                teamKey,
                UserTeamActionType.INVITE_ACCEPTED,
                "팀 초대를 수락했습니다."
        );
    }

    public static InvitationActionResponse rejected(Long invitationKey, Integer teamKey) {
        return new InvitationActionResponse(
                invitationKey,
                teamKey,
                UserTeamActionType.INVITE_REJECTED,
                "팀 초대를 거절했습니다."
        );
    }
    public static InvitationActionResponse expired(Long invitationKey, Integer teamKey) {
        return new InvitationActionResponse(
                invitationKey,
                teamKey,
                UserTeamActionType.INVITE_EXPIRED,
                "팀 초대가 만료됐습니다."
        );
    }
}
