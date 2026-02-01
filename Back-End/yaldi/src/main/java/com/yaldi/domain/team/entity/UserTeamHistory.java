package com.yaldi.domain.team.entity;

import com.yaldi.domain.user.entity.User;
import com.yaldi.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * UserTeamHistory 엔티티
 */
@Entity
@Table(name = "user_team_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTeamHistory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_team_history_key")
    private Long userTeamHistoryKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_key", nullable = false, foreignKey = @ForeignKey(name = "fk_user_team_history_team"))
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_key", nullable = false, foreignKey = @ForeignKey(name = "fk_user_team_history_actor"))
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_key", foreignKey = @ForeignKey(name = "fk_user_team_history_target"))
    private User target;

    @Column(name = "email", length = 255)
    private String email;

    @Convert(converter = UserTeamActionTypeConverter.class)
    @Column(name = "action_type", length = 50, nullable = false)
    private UserTeamActionType actionType;

    @Column(name = "reason", length = 255)
    @Builder.Default
    private String reason = "";

    // 비즈니스 로직
    public boolean isExpired() { // createdAt 기준 3일 후 만료
        return this.getCreatedAt().plusDays(3).isBefore(OffsetDateTime.now());
    }
}
