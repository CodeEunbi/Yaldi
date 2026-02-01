package com.yaldi.domain.project.entity;

import com.yaldi.global.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ProjectMemberRelation 엔티티
 *
 * <p>프로젝트와 멤버 간의 관계 및 역할을 관리합니다.</p>
 *
 * <p>role 필드는 PostgreSQL ENUM 타입(project_member_role_type)을 사용하며,
 * ProjectMemberRoleConverter를 통해 Java Enum과 매핑됩니다.</p>
 */
@Entity
@Table(
        name = "project_member_relations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_member",
                        columnNames = {"project_key", "member_key"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberRelation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_member_relations_key")
    private Long projectMemberRelationKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "member_key", nullable = false)
    private Integer memberKey;

    /**
     * PostgreSQL ENUM 타입: project_member_role_type
     * Java Enum: ProjectMemberRole
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false)
    private ProjectMemberRole role;

    /**
     * 역할 변경
     */
    public void changeRole(ProjectMemberRole newRole) {
        this.role = newRole;
    }
}
