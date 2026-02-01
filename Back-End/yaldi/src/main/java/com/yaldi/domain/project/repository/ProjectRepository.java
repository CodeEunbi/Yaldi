package com.yaldi.domain.project.repository;

import com.yaldi.domain.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 팀의 프로젝트 목록 조회 (페이징)
     */
    List<Project> findByTeamKey(Integer teamKey);

    /**
     * 팀의 프로젝트 목록 조회 (페이징)
     */
    Page<Project> findByTeamKey(Integer teamKey, Pageable pageable);

    /**
     * 사용자가 속한 프로젝트 목록 조회 (페이징)
     * ProjectMemberRelation을 JOIN하여 조회
     */
    @Query("SELECT p FROM Project p " +
           "JOIN ProjectMemberRelation pmr ON p.projectKey = pmr.projectKey " +
           "WHERE pmr.memberKey = :memberKey AND p.deletedAt IS NULL")
    Page<Project> findByMemberKey(@Param("memberKey") Integer memberKey, Pageable pageable);

    /**
     * 삭제된 프로젝트만 조회 (페이징)
     */
    @Query(value = "SELECT * FROM projects WHERE deleted_at IS NOT NULL", nativeQuery = true)
    Page<Project> findDeleted(Pageable pageable);

    /**
     * 특정 팀의 삭제된 프로젝트만 조회 (페이징)
     */
    @Query(value = "SELECT * FROM projects WHERE team_key = :teamKey AND deleted_at IS NOT NULL", nativeQuery = true)
    Page<Project> findDeletedByTeamKey(@Param("teamKey") Integer teamKey, Pageable pageable);

    /**
     * 삭제된 프로젝트 포함 ID로 조회
     */
    @Query(value = "SELECT * FROM projects WHERE project_key = :projectKey", nativeQuery = true)
    Optional<Project> findByIdIncludingDeleted(Long projectKey);

    /**
     * 특정 팀의 활성 프로젝트 수 조회
     */
    @Query("SELECT COUNT(p) FROM Project p WHERE p.teamKey = :teamKey AND p.deletedAt IS NULL")
    long countActiveProjectsByTeamKey(Integer teamKey);
}
