package com.yaldi.global.asyncjob.entity;

import com.yaldi.global.common.BaseCreateOnlyEntity;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 비동기 작업 엔티티
 * Kafka 기반 모든 비동기 작업 통합 관리
 */
@Entity
@Table(name = "async_jobs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncJob extends BaseCreateOnlyEntity {

    @Id
    @Column(name = "job_id", length = 26)
    private String jobId;

    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;  // 'MOCK_DATA', 'DESIGN_VERIFY', 'IMPORT', 'AGENT' (확장 가능)

    @Column(name = "user_key", nullable = false)
    private Integer userKey;

    @Column(name = "reference_key")
    private Long referenceKey;  // versionKey, projectKey 등 (해당 작업이 참조하는 대상의 PK)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AsyncJobStatus status = AsyncJobStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    // 비즈니스 로직
    public void updateStatus(AsyncJobStatus status) {
        this.status = status;
        if (status == AsyncJobStatus.COMPLETED || status == AsyncJobStatus.FAILED) {
            this.completedAt = OffsetDateTime.now();
        }
    }

    public void fail(String errorMessage) {
        this.status = AsyncJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
    }
}
