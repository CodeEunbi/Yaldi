package com.yaldi.global.scheduler.service;

import com.yaldi.domain.project.repository.ProjectRepository;
import com.yaldi.domain.team.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Soft Delete된 데이터를 물리적으로 삭제하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HardDeleteService {

    private final EntityManager entityManager;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;

    /**
     * 30일 이상 지난 soft deleted 프로젝트를 물리 삭제
     *
     * @param daysThreshold 삭제 기준 일수 (기본 30일)
     * @return 삭제된 프로젝트 수
     */
    @Transactional
    public int hardDeleteProjects(int daysThreshold) {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(daysThreshold);

        log.info("Starting hard delete for projects deleted before: {}", threshold);

        // Native Query로 물리 삭제
        // Note: DB 트리거는 soft delete만 처리하므로, 물리 삭제는 순서대로 직접 처리
        // project_member_relations는 ON DELETE CASCADE로 자동 삭제됨

        int deletedCount = 0;

        // 1. 프로젝트 관련 자식 데이터 삭제 (순서 중요!)
        deletedCount += deleteByNativeQuery(
            "DELETE FROM mock_data WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM data_model_erd_column_relations WHERE model_key IN " +
            "(SELECT model_key FROM data_models WHERE deleted_at IS NOT NULL AND deleted_at < :threshold)",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM data_models WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM versions WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM replies WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM comments WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM erd_relations WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM erd_columns WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM erd_tables WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        deletedCount += deleteByNativeQuery(
            "DELETE FROM snapshots WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        // 2. 프로젝트 삭제
        int projectCount = deleteByNativeQuery(
            "DELETE FROM projects WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );
        deletedCount += projectCount;

        log.info("Hard delete completed: {} records deleted ({} projects)", deletedCount, projectCount);

        return projectCount;
    }

    /**
     * 30일 이상 지난 soft deleted 팀을 물리 삭제
     *
     * @param daysThreshold 삭제 기준 일수 (기본 30일)
     * @return 삭제된 팀 수
     */
    @Transactional
    public int hardDeleteTeams(int daysThreshold) {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(daysThreshold);

        log.info("Starting hard delete for teams deleted before: {}", threshold);

        // 팀 삭제 전에 프로젝트가 먼저 삭제되어야 함
        int teamCount = deleteByNativeQuery(
            "DELETE FROM teams WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        log.info("Hard delete completed: {} teams deleted", teamCount);

        return teamCount;
    }

    /**
     * 30일 이상 지난 soft deleted 사용자를 물리 삭제
     *
     * @param daysThreshold 삭제 기준 일수 (기본 30일)
     * @return 삭제된 사용자 수
     */
    @Transactional
    public int hardDeleteUsers(int daysThreshold) {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(daysThreshold);

        log.info("Starting hard delete for users deleted before: {}", threshold);

        // 사용자 삭제 시 이미 트리거로 연관 데이터가 처리되었음
        int userCount = deleteByNativeQuery(
            "DELETE FROM users WHERE deleted_at IS NOT NULL AND deleted_at < :threshold",
            threshold
        );

        log.info("Hard delete completed: {} users deleted", userCount);

        return userCount;
    }

    /**
     * Native Query 실행 헬퍼 메소드
     */
    private int deleteByNativeQuery(String sql, OffsetDateTime threshold) {
        try {
            int count = entityManager.createNativeQuery(sql)
                    .setParameter("threshold", threshold)
                    .executeUpdate();

            if (count > 0) {
                log.debug("Deleted {} records: {}", count, sql.substring(0, Math.min(50, sql.length())));
            }

            return count;
        } catch (Exception e) {
            log.error("Failed to execute delete query: {}", sql, e);
            throw e;
        }
    }
}
