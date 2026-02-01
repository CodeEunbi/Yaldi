package com.yaldi.domain.project.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

/**
 * Project 엔티티
 */
@Entity
@Table(name = "projects")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_key")
    private Long projectKey;

    @Column(name = "team_key", nullable = false)
    private Integer teamKey;

    @Column(name = "name", length = 25, nullable = false)
    private String name;

    @Column(name = "description", length = 1000, nullable = false)
    @Builder.Default
    private String description = "";

    @Column(name = "image_url", length = 10000)
    private String imageUrl;

    @Column(name = "last_activity_at")
    private OffsetDateTime lastActivityAt;

    // JPA 콜백
    @PrePersist
    protected void onCreate() {
        this.lastActivityAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastActivityAt = OffsetDateTime.now();
    }

    // 비즈니스 로직
    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
