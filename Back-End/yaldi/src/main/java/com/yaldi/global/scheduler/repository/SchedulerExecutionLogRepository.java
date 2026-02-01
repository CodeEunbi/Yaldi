package com.yaldi.global.scheduler.repository;

import com.yaldi.global.scheduler.entity.SchedulerExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 스케줄러 실행 이력 Repository
 */
@Repository
public interface SchedulerExecutionLogRepository extends JpaRepository<SchedulerExecutionLog, Long> {

    /**
     * Job 이름으로 최근 실행 이력 조회
     */
    List<SchedulerExecutionLog> findByJobNameOrderByStartedAtDesc(String jobName);

    /**
     * 특정 기간의 실행 이력 조회
     */
    List<SchedulerExecutionLog> findByStartedAtBetweenOrderByStartedAtDesc(
            OffsetDateTime start, OffsetDateTime end);

    /**
     * 실패한 실행 이력 조회
     */
    List<SchedulerExecutionLog> findByStatusOrderByStartedAtDesc(
            SchedulerExecutionLog.ExecutionStatus status);
}
