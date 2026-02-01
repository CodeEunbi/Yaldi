package com.yaldi.domain.project.repository;

import com.yaldi.domain.project.entity.ProjectMemberActionType;
import com.yaldi.domain.project.entity.ProjectMemberHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMemberHistoryRepository extends JpaRepository<ProjectMemberHistory, Long> {

    /**
     * 특정 프로젝트의 히스토리 조회
     */
    List<ProjectMemberHistory> findByProjectKey(Long projectKey);

    /**
     * 특정 프로젝트의 히스토리를 생성일 기준 내림차순 조회
     */
    List<ProjectMemberHistory> findByProjectKeyOrderByCreatedAtDesc(Long projectKey);

    /**
     * 특정 프로젝트의 히스토리를 생성일 기준 내림차순 조회 (페이징)
     */
    Page<ProjectMemberHistory> findByProjectKeyOrderByCreatedAtDesc(Long projectKey, Pageable pageable);

    /**
     * 특정 사용자가 행위자인 히스토리 조회
     */
    List<ProjectMemberHistory> findByActorKey(Integer actorKey);

    /**
     * 특정 사용자가 대상인 히스토리 조회
     */
    List<ProjectMemberHistory> findByTargetKey(Integer targetKey);

    /**
     * 특정 액션 타입으로 히스토리 조회
     */
    List<ProjectMemberHistory> findByActionType(ProjectMemberActionType actionType);
}
