package com.yaldi.domain.version.repository;

import com.yaldi.domain.version.entity.DesignVerificationStatus;
import com.yaldi.domain.version.entity.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface VersionRepository extends JpaRepository<Version, Long> {

    /**
     * 프로젝트의 버전 목록 조회 (최신순)
     */
    List<Version> findByProjectKeyOrderByCreatedAtDesc(Long projectKey);

    /**
     * 프로젝트의 버전 목록 조회 (페이지네이션, version_key 기준 최신순)
     */
    Page<Version> findByProjectKeyOrderByVersionKeyDesc(Long projectKey, Pageable pageable);

    /**
     * 프로젝트의 Public 버전 목록 조회 (최신순)
     */
    List<Version> findByProjectKeyAndIsPublicTrueOrderByCreatedAtDesc(Long projectKey);

    //Vector 업데이트 (pgvector 타입 캐스팅)
    @Transactional
    @Modifying
    @Query(value = "UPDATE versions SET vector = CAST(:vectorString AS vector) WHERE version_key = :versionKey", nativeQuery = true)
    void updateVector(@Param("versionKey") Long versionKey, @Param("vectorString") String vectorString);
}
