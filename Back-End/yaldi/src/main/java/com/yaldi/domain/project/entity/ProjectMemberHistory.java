package com.yaldi.domain.project.entity;

import com.yaldi.global.common.BaseCreateOnlyEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ProjectMemberHistory 엔티티
 */
@Entity
@Table(name = "project_member_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberHistory extends BaseCreateOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_member_history_key")
    private Long projectMemberHistoryKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "actor_key", nullable = false)
    private Integer actorKey;

    @Column(name = "target_key", nullable = false)
    private Integer targetKey;

    @Convert(converter = ProjectMemberActionTypeConverter.class)
    @Column(name = "action_type", length = 50, nullable = false)
    private ProjectMemberActionType actionType;
}
