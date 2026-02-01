package com.yaldi.domain.project.repository;

import com.yaldi.domain.project.entity.ProjectMemberRole;
import com.yaldi.domain.project.entity.ProjectMemberRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRelationRepository extends JpaRepository<ProjectMemberRelation, Long> {

    /**
     * 프로젝트의 멤버 목록 조회
     */
    List<ProjectMemberRelation> findByProjectKey(Long projectKey);

    /**
     * 프로젝트의 멤버 목록 조회 (페이징)
     */
    Page<ProjectMemberRelation> findByProjectKey(Long projectKey, Pageable pageable);

    /**
     * 멤버가 속한 프로젝트 목록 조회
     */
    List<ProjectMemberRelation> findByMemberKey(Integer memberKey);

    /**
     * 멤버가 속한 프로젝트 목록 조회 (페이징)
     */
    Page<ProjectMemberRelation> findByMemberKey(Integer memberKey, Pageable pageable);

    /**
     * 특정 프로젝트와 멤버의 관계 조회
     */
    Optional<ProjectMemberRelation> findByProjectKeyAndMemberKey(Long projectKey, Integer memberKey);

    /**
     * 특정 멤버의 여러 프로젝트에 대한 관계 조회 (배치 조회)
     */
    List<ProjectMemberRelation> findByMemberKeyAndProjectKeyIn(Integer memberKey, List<Long> projectKeys);

    /**
     * 특정 프로젝트와 멤버의 관계 존재 여부
     */
    boolean existsByProjectKeyAndMemberKey(Long projectKey, Integer memberKey);

    /**
     * 특정 프로젝트의 특정 역할을 가진 멤버 목록 조회
     */
    List<ProjectMemberRelation> findByProjectKeyAndRole(Long projectKey, ProjectMemberRole role);

    /**
     * 특정 프로젝트의 관계 삭제
     */
    void deleteByProjectKey(Long projectKey);
}
