package com.yaldi.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "팀 멤버 정보 (오너 양도용)")
public record TeamMemberInfo(

        @Schema(description = "유저 Key", example = "2")
        Integer userKey,

        @Schema(description = "닉네임", example = "김서원")
        String nickname
) {
}
