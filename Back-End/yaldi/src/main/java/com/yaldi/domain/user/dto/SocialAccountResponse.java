package com.yaldi.domain.user.dto;

import com.yaldi.domain.user.entity.Provider;
import com.yaldi.domain.user.entity.UserSocialAccount;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 소셜 계정 정보 응답 DTO
 */
@Getter
@Builder
public class SocialAccountResponse {

    private Integer socialAccountKey;
    private Provider provider;
    private String oauthUserId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /**
     * UserSocialAccount 엔티티를 SocialAccountResponse DTO로 변환
     */
    public static SocialAccountResponse from(UserSocialAccount socialAccount) {
        return SocialAccountResponse.builder()
                .socialAccountKey(socialAccount.getSocialAccountKey())
                .provider(socialAccount.getProvider())
                .oauthUserId(socialAccount.getOauthUserId())
                .createdAt(socialAccount.getCreatedAt())
                .updatedAt(socialAccount.getUpdatedAt())
                .build();
    }

    /**
     * UserSocialAccount 리스트를 SocialAccountResponse 리스트로 변환
     */
    public static List<SocialAccountResponse> fromList(List<UserSocialAccount> socialAccounts) {
        return socialAccounts.stream()
                .map(SocialAccountResponse::from)
                .collect(Collectors.toList());
    }
}
