package com.yaldi.global.scheduler.entity;

import com.yaldi.global.common.BaseCreateOnlyEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 스케줄러 실행 이력 엔티티
 */
@Entity
@Table(name = "scheduler_execution_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerExecutionLog extends BaseCreateOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_key")
    private Long logKey;

    @Column(name = "job_name", length = 100, nullable = false)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "deleted_count")
    @Builder.Default
    private Integer deletedCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 실행 완료 처리
     */
    public void complete(int deletedCount) {
        this.completedAt = OffsetDateTime.now();
        this.executionTimeMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        this.deletedCount = deletedCount;
        this.status = ExecutionStatus.SUCCESS;
    }

    /**
     * 실행 실패 처리
     */
    public void fail(String errorMessage) {
        this.completedAt = OffsetDateTime.now();
        this.executionTimeMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        this.status = ExecutionStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 스케줄러 실행 상태
     */
    public enum ExecutionStatus {
        RUNNING,    // 실행 중
        SUCCESS,    // 성공
        FAILED      // 실패
    }
}
