package com.yaldi.domain.erd.repository;

import com.yaldi.domain.erd.entity.ErdColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErdColumnRepository extends JpaRepository<ErdColumn, Long> {

    /**
     * 테이블의 컬럼 목록 조회
     */
    List<ErdColumn> findByTableKey(Long tableKey);

    /**
     * 삭제된 ERD 컬럼 포함 전체 조회
     */
    @Query(value = "SELECT * FROM erd_columns", nativeQuery = true)
    List<ErdColumn> findAllIncludingDeleted();

    /**
     * 삭제된 ERD 컬럼 포함 ID로 조회
     */
    @Query(value = "SELECT * FROM erd_columns WHERE column_key = :columnKey", nativeQuery = true)
    Optional<ErdColumn> findByIdIncludingDeleted(Long columnKey);

    /**
     * 활성 ERD 컬럼 조회 (삭제되지 않은)
     */
    @Query("SELECT c FROM ErdColumn c WHERE c.columnKey = :columnKey AND c.deletedAt IS NULL")
    Optional<ErdColumn> findActiveColumnById(Long columnKey);

    /**
     * 테이블의 Primary Key 컬럼 조회
     */
    List<ErdColumn> findByTableKeyAndIsPrimaryKeyTrue(Long tableKey);

    /**
     * 테이블의 Foreign Key 컬럼 조회
     */
    List<ErdColumn> findByTableKeyAndIsForeignKeyTrue(Long tableKey);

    /**
     * 프로젝트의 모든 컬럼 조회 (1+N 쿼리 방지)
     * ErdTable과 JOIN하여 한 번에 조회
     */
    @Query("SELECT c FROM ErdColumn c " +
           "JOIN ErdTable t ON c.tableKey = t.tableKey " +
           "WHERE t.projectKey = :projectKey " +
           "AND c.deletedAt IS NULL " +
           "AND t.deletedAt IS NULL")
    List<ErdColumn> findByProjectKey(Long projectKey);
}
