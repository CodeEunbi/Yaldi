package com.yaldi.domain.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 검색 결과")
public record UserSearchResult(
        @Schema(description = "유저 Key", example = "1")
        Integer userKey,

        @Schema(description = "닉네임", example = "yaldi")
        String nickname,

        @Schema(description = "이메일", example = "yaldi@example.com")
        String email,

        @Schema(description = "초대 상태", example = "INVITABLE")
        InviteStatus status
) {

    //UserSearchResult에서만 쓰이는 inner enum
    public enum InviteStatus {
        INVITABLE,          // 초대 가능
        ALREADY_MEMBER,     // 이미 팀 멤버
        ALREADY_INVITED     // 이미 초대 보냄 (대기 중)
    }
}
