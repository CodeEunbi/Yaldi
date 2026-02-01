package com.yaldi.domain.user.dto;

import com.yaldi.domain.user.entity.UserSocialAccount;
import com.yaldi.global.response.PageMeta;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 소셜 계정 목록 응답 DTO (메타 정보 포함)
 */
@Getter
@Builder
public class SocialAccountListResponse {

    private List<SocialAccountResponse> data;
    private PageMeta meta;

    /**
     * UserSocialAccount 리스트를 SocialAccountListResponse로 변환
     */
    public static SocialAccountListResponse from(List<UserSocialAccount> socialAccounts) {
        List<SocialAccountResponse> data = SocialAccountResponse.fromList(socialAccounts);
        PageMeta meta = PageMeta.of(socialAccounts.size());

        return SocialAccountListResponse.builder()
                .data(data)
                .meta(meta)
                .build();
    }
}
