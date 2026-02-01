package com.yaldi.domain.version.repository;

import com.yaldi.domain.version.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {

    /**
     * 프로젝트의 스냅샷 목록 조회 (최신순)
     */
    List<Snapshot> findByProjectKeyOrderByCreatedAtDesc(Long projectKey);

    /**
     * 프로젝트의 특정 이름 스냅샷 조회
     */
    Optional<Snapshot> findByProjectKeyAndName(Long projectKey, String name);

    /**
     * 특정 사용자가 생성한 스냅샷 목록 조회
     */
    List<Snapshot> findByCreatedByOrderByCreatedAtDesc(Integer createdBy);

    /**
     * 프로젝트의 스냅샷 개수 조회
     */
    long countByProjectKey(Long projectKey);

    /**
     * 프로젝트의 특정 이름 스냅샷 존재 여부
     */
    boolean existsByProjectKeyAndName(Long projectKey, String name);
}
