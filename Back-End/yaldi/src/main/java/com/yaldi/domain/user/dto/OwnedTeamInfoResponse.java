package com.yaldi.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "오너인 팀 정보 (탈퇴 시 오너 이양 필요)")
public record OwnedTeamInfoResponse(

        @Schema(description = "팀 Key", example = "1")
        Integer teamKey,

        @Schema(description = "팀 이름", example = "알파팀")
        String name,

        @Schema(description = "팀 멤버 수", example = "5")
        Long memberCount,

        @Schema(description = "팀 멤버 목록 (본인 제외, 양도 가능한 멤버만)")
        List<TeamMemberInfo> members

) {
}
