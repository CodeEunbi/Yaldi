package com.yaldi.domain.erd.repository;

import com.yaldi.domain.erd.entity.ErdTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErdTableRepository extends JpaRepository<ErdTable, Long> {

    /**
     * 프로젝트의 ERD 테이블 목록 조회
     */
    List<ErdTable> findByProjectKey(Long projectKey);

    /**
     * 삭제된 ERD 테이블 포함 전체 조회
     */
    @Query(value = "SELECT * FROM erd_tables", nativeQuery = true)
    List<ErdTable> findAllIncludingDeleted();

    /**
     * 삭제된 ERD 테이블 포함 ID로 조회
     */
    @Query(value = "SELECT * FROM erd_tables WHERE table_key = :tableKey", nativeQuery = true)
    Optional<ErdTable> findByIdIncludingDeleted(Long tableKey);

    /**
     * 활성 ERD 테이블 조회 (삭제되지 않은)
     */
    @Query("SELECT t FROM ErdTable t WHERE t.tableKey = :tableKey AND t.deletedAt IS NULL")
    Optional<ErdTable> findActiveTableById(Long tableKey);

    /**
     * 프로젝트의 물리적 이름으로 테이블 조회
     */
    Optional<ErdTable> findByProjectKeyAndPhysicalName(Long projectKey, String physicalName);
}
