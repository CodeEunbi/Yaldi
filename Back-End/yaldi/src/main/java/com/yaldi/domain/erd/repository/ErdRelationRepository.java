package com.yaldi.domain.erd.repository;

import com.yaldi.domain.erd.entity.ErdRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErdRelationRepository extends JpaRepository<ErdRelation, Long> {

    /**
     * 프로젝트의 ERD 관계 목록 조회
     */
    List<ErdRelation> findByProjectKey(Long projectKey);

    /**
     * From 테이블의 관계 목록 조회
     */
    List<ErdRelation> findByFromTableKey(Long fromTableKey);

    /**
     * To 테이블의 관계 목록 조회
     */
    List<ErdRelation> findByToTableKey(Long toTableKey);

    /**
     * 삭제된 ERD 관계 포함 전체 조회
     */
    @Query(value = "SELECT * FROM erd_relations", nativeQuery = true)
    List<ErdRelation> findAllIncludingDeleted();

    /**
     * 삭제된 ERD 관계 포함 ID로 조회
     */
    @Query(value = "SELECT * FROM erd_relations WHERE relation_key = :relationKey", nativeQuery = true)
    Optional<ErdRelation> findByIdIncludingDeleted(Long relationKey);

    /**
     * 활성 ERD 관계 조회 (삭제되지 않은)
     */
    @Query("SELECT r FROM ErdRelation r WHERE r.relationKey = :relationKey AND r.deletedAt IS NULL")
    Optional<ErdRelation> findActiveRelationById(Long relationKey);
}
