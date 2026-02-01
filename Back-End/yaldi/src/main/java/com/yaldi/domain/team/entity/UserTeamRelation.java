package com.yaldi.domain.team.entity;

import com.yaldi.domain.user.entity.User;
import com.yaldi.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * UserTeamRelation 엔티티
 */
@Entity
@Table(
    name = "user_team_relations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_team",
            columnNames = {"user_key", "team_key"}
        )
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTeamRelation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_team_relation_key")
    private Integer userTeamRelationKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_key", nullable = false, foreignKey = @ForeignKey(name = "fk_user_team_relation_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_key", nullable = false, foreignKey = @ForeignKey(name = "fk_user_team_relation_team"))
    private Team team;
}
