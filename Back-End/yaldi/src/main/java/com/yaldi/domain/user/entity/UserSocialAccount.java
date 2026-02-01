package com.yaldi.domain.user.entity;

import com.yaldi.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UserSocialAccount 엔티티
 *
 * Note: 이 테이블에는 추가 인덱스가 정의되어 있지 않습니다.
 * UNIQUE 제약조건이 자동으로 인덱스를 생성합니다.
 */
@Entity
@Table(
    name = "user_social_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_provider_oauth_user_id",
            columnNames = {"provider", "oauth_user_id"}
        )
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSocialAccount extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_key")
    private Integer socialAccountKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_key", nullable = false)
    private User user;

    @Convert(converter = ProviderConverter.class)
    @Column(name = "provider", length = 50, nullable = false)
    private Provider provider;

    @Column(name = "oauth_user_id", length = 255, nullable = false)
    private String oauthUserId;

    // userKey getter 편의 메서드 (User 객체 로딩 없이 사용)
    public Integer getUserKey() {
        return user != null ? user.getUserKey() : null;
    }
}
