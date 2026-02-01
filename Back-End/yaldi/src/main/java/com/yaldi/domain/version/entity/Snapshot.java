package com.yaldi.domain.version.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * Snapshot 엔티티
 */
@Entity
@Table(
    name = "snapshots",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_project_snapshot_name",
            columnNames = {"project_key", "name"}
        )
    }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Snapshot extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_key")
    private Long snapshotKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "name", length = 500, nullable = false)
    @Builder.Default
    private String name = "";

    @Type(JsonBinaryType.class)
    @Column(name = "schema_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> schemaData;
}
