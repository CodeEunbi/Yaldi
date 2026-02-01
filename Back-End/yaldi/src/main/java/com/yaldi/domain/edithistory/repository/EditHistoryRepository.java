package com.yaldi.domain.edithistory.repository;

import com.yaldi.domain.edithistory.entity.EditHistoryActionType;
import com.yaldi.domain.edithistory.entity.EditHistoryTargetType;
import com.yaldi.domain.edithistory.entity.EditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EditHistoryRepository extends JpaRepository<EditHistory, Long> {

    /**
     * 프로젝트의 편집 히스토리 조회 (최신순)
     */
    List<EditHistory> findByProjectKeyOrderByCreatedAtDesc(Long projectKey);

    /**
     * 특정 사용자의 편집 히스토리 조회
     */
    List<EditHistory> findByUserKeyOrderByCreatedAtDesc(Integer userKey);

    /**
     * 특정 타겟의 편집 히스토리 조회
     */
    List<EditHistory> findByTargetKeyOrderByCreatedAtDesc(Long targetKey);

    /**
     * 특정 타겟 타입의 편집 히스토리 조회
     */
    List<EditHistory> findByTargetType(EditHistoryTargetType targetType);

    /**
     * 특정 액션 타입의 편집 히스토리 조회
     */
    List<EditHistory> findByActionType(EditHistoryActionType actionType);

    /**
     * 프로젝트의 특정 타겟 타입 편집 히스토리 조회
     */
    List<EditHistory> findByProjectKeyAndTargetType(Long projectKey, EditHistoryTargetType targetType);
}
