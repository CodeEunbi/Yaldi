package com.yaldi.global.scheduler;

import com.yaldi.global.scheduler.entity.SchedulerExecutionLog;
import com.yaldi.global.scheduler.repository.SchedulerExecutionLogRepository;
import com.yaldi.global.scheduler.service.HardDeleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Soft Delete된 데이터를 주기적으로 물리 삭제하는 스케줄러
 *
 * <p>매일 새벽 3시에 실행되며, 30일 이상 지난 soft deleted 데이터를 물리적으로 삭제합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SoftDeleteCleanupScheduler {

    private static final String JOB_NAME_PROJECTS = "CLEANUP_SOFT_DELETED_PROJECTS";
    private static final String JOB_NAME_TEAMS = "CLEANUP_SOFT_DELETED_TEAMS";
    private static final String JOB_NAME_USERS = "CLEANUP_SOFT_DELETED_USERS";
    private static final int DAYS_THRESHOLD = 30;

    private final HardDeleteService hardDeleteService;
    private final SchedulerExecutionLogRepository logRepository;

    /**
     * 프로젝트 물리 삭제 스케줄러
     * 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupSoftDeletedProjects() {
        executeJob(JOB_NAME_PROJECTS, () ->
            hardDeleteService.hardDeleteProjects(DAYS_THRESHOLD)
        );
    }

    /**
     * 팀 물리 삭제 스케줄러
     * 매일 새벽 3시 10분에 실행 (프로젝트 삭제 후)
     */
    @Scheduled(cron = "0 10 3 * * *")
    public void cleanupSoftDeletedTeams() {
        executeJob(JOB_NAME_TEAMS, () ->
            hardDeleteService.hardDeleteTeams(DAYS_THRESHOLD)
        );
    }

    /**
     * 사용자 물리 삭제 스케줄러
     * 매일 새벽 3시 20분에 실행
     */
    @Scheduled(cron = "0 20 3 * * *")
    public void cleanupSoftDeletedUsers() {
        executeJob(JOB_NAME_USERS, () ->
            hardDeleteService.hardDeleteUsers(DAYS_THRESHOLD)
        );
    }

    /**
     * Job 실행 템플릿 메소드
     */
    private void executeJob(String jobName, JobExecutor executor) {
        // 실행 로그 생성
        SchedulerExecutionLog log = SchedulerExecutionLog.builder()
                .jobName(jobName)
                .status(SchedulerExecutionLog.ExecutionStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .build();

        log = logRepository.save(log);

        try {
            this.log.info("Starting scheduled job: {}", jobName);

            // Job 실행
            int deletedCount = executor.execute();

            // 성공 처리
            log.complete(deletedCount);
            logRepository.save(log);

            this.log.info("Scheduled job completed successfully: {} (deleted: {})", jobName, deletedCount);

        } catch (Exception e) {
            // 실패 처리
            log.fail(e.getMessage());
            logRepository.save(log);

            this.log.error("Scheduled job failed: {}", jobName, e);
        }
    }

    /**
     * Job 실행 인터페이스
     */
    @FunctionalInterface
    private interface JobExecutor {
        int execute();
    }
}
