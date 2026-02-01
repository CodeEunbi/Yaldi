package com.yaldi.domain.user.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

/**
 * User 엔티티
 *
 * Indexes (managed by 03_indexes.sql):
 * - idx_users_email_active: UNIQUE INDEX ON email WHERE deleted_at IS NULL
 * - idx_users_nickname_active: UNIQUE INDEX ON nickname WHERE deleted_at IS NULL
 * - idx_users_deleted_at: INDEX ON deleted_at WHERE deleted_at IS NULL
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uk_user_nickname", columnNames = {"nickname"})
    }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_key")
    private Integer userKey;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "nickname", length = 10, nullable = false)
    private String nickname;

    // User -> UserSocialAccount 양방향 관계 (1:N)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserSocialAccount> socialAccounts = new ArrayList<>();

    // 양방향 관계 편의 메서드
    public void addSocialAccount(UserSocialAccount socialAccount) {
        this.socialAccounts.add(socialAccount);
    }

    public void removeSocialAccount(UserSocialAccount socialAccount) {
        this.socialAccounts.remove(socialAccount);
    }

    // 비즈니스 로직
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
