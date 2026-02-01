package com.yaldi.domain.datamodel.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

/**
 * DataModel 엔티티
 */
@Entity
@Table(
    name = "data_models",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_project_model_name",
            columnNames = {"project_key", "name"}
        )
    }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataModel extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_key")
    private Long modelKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "name", length = 500, nullable = false)
    private String name;

    @Convert(converter = DataModelTypeConverter.class)
    @Column(name = "type", length = 50, nullable = false)
    private DataModelType type;

    @Column(name = "source_table_key")
    private Long sourceTableKey;

    @Column(name = "last_synced_at", nullable = false)
    private OffsetDateTime lastSyncedAt;

    // 비즈니스 로직
    public void updateName(String name) {
        this.name = name;
    }

    public void updateType(DataModelType type) {
        this.type = type;
    }

    public void updateLastSyncedAt(OffsetDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
