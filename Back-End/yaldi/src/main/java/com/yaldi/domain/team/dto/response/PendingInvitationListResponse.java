package com.yaldi.domain.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "팀 대기 중인 초대 목록 응답")
public record PendingInvitationListResponse(

        @Schema(description = "대기 중인 초대 목록")
        List<TeamInvitationResponse> invitations

) {

    public static PendingInvitationListResponse from(List<TeamInvitationResponse> invitations) {
        return new PendingInvitationListResponse(invitations);
    }
}
