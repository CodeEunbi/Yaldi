package com.yaldi.domain.team.entity;

import com.yaldi.domain.user.entity.User;
import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Team 엔티티
 */
@Entity
@Table(name = "teams")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_key")
    private Integer teamKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owned_by", nullable = false, foreignKey = @ForeignKey(name = "fk_team_owner"))
    private User owner;

    @Column(name = "name", length = 25, nullable = false)
    private String name;

    // 비즈니스 로직
    public void updateName(String name) {
        this.name = name;
    }

    public void updateOwner(User newOwner) {
        this.owner = newOwner;
    }
}
