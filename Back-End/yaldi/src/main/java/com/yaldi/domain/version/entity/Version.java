package com.yaldi.domain.version.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import com.yaldi.global.asyncjob.entity.AsyncJob;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "versions")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Version extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_key")
    private Long versionKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private AsyncJob asyncJob;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "name", length = 255, nullable = false)
    @Builder.Default
    private String name = "";

    @Type(JsonBinaryType.class)
    @Column(name = "schema_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> schemaData;

    @Column(name = "description", length = 1000, nullable = false)
    @Builder.Default
    private String description = "";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "design_verification_status", nullable = false)
    @Builder.Default
    private DesignVerificationStatus designVerificationStatus = DesignVerificationStatus.QUEUED;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Type(JsonBinaryType.class)
    @Column(name = "verification_result", columnDefinition = "jsonb")
    private Map<String, Object> verificationResult;

    @Column(name = "vector", columnDefinition = "vector(1536)", insertable = false, updatable = false)
    private String vector;

    // 비즈니스 로직
    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateSchemaData(Map<String, Object> schemaData) {
        this.schemaData = schemaData;
    }

    public void updateVerificationStatus(DesignVerificationStatus status) {
        this.designVerificationStatus = status;
    }

    public void updateVerificationResult(Map<String, Object> verificationResult) {
        this.verificationResult = verificationResult;
    }

    public void updateAsyncJob(AsyncJob asyncJob) {
        this.asyncJob = asyncJob;
    }

    public void updateVector(String vector) {
        this.vector = vector;
    }

    public void makePublic() {
        this.isPublic = true;
    }

    public void makePrivate() {
        this.isPublic = false;
    }
}
