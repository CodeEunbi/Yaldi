package com.yaldi.domain.team.dto.response;

import com.yaldi.domain.team.entity.UserTeamHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "팀 초대 정보 응답")
public record TeamInvitationResponse(

        @Schema(description = "초대 기록 Key", example = "1")
        Long invitationKey,

        @Schema(description = "팀 Key", example = "5")
        Integer teamKey,

        @Schema(description = "팀 이름", example = "개발팀")
        String teamName,

        @Schema(description = "초대한 사용자 Key", example = "10")
        Integer inviterUserKey,

        @Schema(description = "초대한 사용자 닉네임", example = "김철수")
        String inviterNickname,

        @Schema(description = "초대받은 사용자 Key", example = "20")
        Integer invitedUserKey,

        @Schema(description = "초대받은 사용자 닉네임", example = "이영희")
        String invitedNickname,

        @Schema(description = "초대받은 사용자 이메일", example = "user@example.com")
        String invitedEmail,

        @Schema(description = "초대 상태", example = "INVITE_SENT")
        String status,

        @Schema(description = "초대 생성 시간", example = "2025-01-15T10:30:00+09:00")
        OffsetDateTime createdAt
) {

    public static TeamInvitationResponse from(UserTeamHistory history, String teamName, String inviterNickname) {
        return new TeamInvitationResponse(
                history.getUserTeamHistoryKey(),
                history.getTeam().getTeamKey(),
                teamName,
                history.getActor().getUserKey(),
                inviterNickname,
                history.getTarget().getUserKey(),
                history.getTarget().getNickname(),
                history.getEmail(),
                history.getActionType().getValue(),
                history.getCreatedAt()
        );
    }
}
