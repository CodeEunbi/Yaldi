package com.yaldi.domain.edithistory.service;

import com.yaldi.domain.edithistory.entity.EditHistory;
import com.yaldi.domain.edithistory.entity.EditHistoryActionType;
import com.yaldi.domain.edithistory.entity.EditHistoryTargetType;
import com.yaldi.domain.edithistory.repository.EditHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EditHistory 서비스
 * ERD의 모든 변경 사항을 기록하고 조회하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EditHistoryService {

    private final EditHistoryRepository editHistoryRepository;

    /**
     * 편집 히스토리 기록
     */
    @Transactional
    public void recordHistory(
            Integer userKey,
            Long projectKey,
            Long targetKey,
            EditHistoryTargetType targetType,
            EditHistoryActionType actionType,
            Map<String, Object> beforeState,
            Map<String, Object> afterState
    ) {
        Map<String, Object> delta = calculateDelta(beforeState, afterState);

        EditHistory history = EditHistory.builder()
                .userKey(userKey)
                .projectKey(projectKey)
                .targetKey(targetKey)
                .targetType(targetType)
                .actionType(actionType)
                .delta(delta)
                .beforeState(beforeState)
                .afterState(afterState)
                .build();

        editHistoryRepository.save(history);
        log.info("Recorded edit history: user={}, project={}, target={}, type={}, action={}",
                userKey, projectKey, targetKey, targetType, actionType);
    }

    /**
     * 편집 히스토리 기록 (간편 버전 - delta만 사용)
     */
    @Transactional
    public void recordHistoryWithDelta(
            Integer userKey,
            Long projectKey,
            Long targetKey,
            EditHistoryTargetType targetType,
            EditHistoryActionType actionType,
            Map<String, Object> delta
    ) {
        EditHistory history = EditHistory.builder()
                .userKey(userKey)
                .projectKey(projectKey)
                .targetKey(targetKey)
                .targetType(targetType)
                .actionType(actionType)
                .delta(delta)
                .build();

        editHistoryRepository.save(history);
        log.info("Recorded edit history (delta only): user={}, project={}, target={}, type={}, action={}",
                userKey, projectKey, targetKey, targetType, actionType);
    }

    /**
     * 프로젝트의 편집 히스토리 조회 (최신순)
     */
    public List<EditHistory> getHistoryByProject(Long projectKey) {
        return editHistoryRepository.findByProjectKeyOrderByCreatedAtDesc(projectKey);
    }

    /**
     * 사용자의 편집 히스토리 조회
     */
    public List<EditHistory> getHistoryByUser(Integer userKey) {
        return editHistoryRepository.findByUserKeyOrderByCreatedAtDesc(userKey);
    }

    /**
     * 특정 타겟의 편집 히스토리 조회
     */
    public List<EditHistory> getHistoryByTarget(Long targetKey) {
        return editHistoryRepository.findByTargetKeyOrderByCreatedAtDesc(targetKey);
    }

    /**
     * 프로젝트의 특정 타입 편집 히스토리 조회
     */
    public List<EditHistory> getHistoryByProjectAndType(Long projectKey, EditHistoryTargetType targetType) {
        return editHistoryRepository.findByProjectKeyAndTargetType(projectKey, targetType);
    }

    /**
     * Delta 계산 (beforeState와 afterState의 차이)
     * null-safe하게 처리
     */
    private Map<String, Object> calculateDelta(Map<String, Object> beforeState, Map<String, Object> afterState) {
        Map<String, Object> delta = new HashMap<>();

        if (afterState == null) {
            return delta;
        }

        if (beforeState == null) {
            // beforeState가 null이면 모든 afterState가 변경사항
            return new HashMap<>(afterState);
        }

        // 변경된 필드만 delta에 추가
        afterState.forEach((key, newValue) -> {
            Object oldValue = beforeState.get(key);
            if (oldValue == null && newValue != null) {
                delta.put(key, newValue);
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                delta.put(key, newValue);
            }
        });

        return delta;
    }

    /**
     * 테이블 생성 히스토리 기록
     */
    @Transactional
    public void recordTableCreation(Integer userKey, Long projectKey, Long tableKey, Map<String, Object> state) {
        recordHistory(userKey, projectKey, tableKey, EditHistoryTargetType.TABLE,
                     EditHistoryActionType.ADD, null, state);
    }

    /**
     * 테이블 수정 히스토리 기록
     */
    @Transactional
    public void recordTableUpdate(Integer userKey, Long projectKey, Long tableKey,
                                  Map<String, Object> beforeState, Map<String, Object> afterState) {
        recordHistory(userKey, projectKey, tableKey, EditHistoryTargetType.TABLE,
                     EditHistoryActionType.UPDATE, beforeState, afterState);
    }

    /**
     * 테이블 삭제 히스토리 기록
     */
    @Transactional
    public void recordTableDeletion(Integer userKey, Long projectKey, Long tableKey, Map<String, Object> state) {
        recordHistory(userKey, projectKey, tableKey, EditHistoryTargetType.TABLE,
                     EditHistoryActionType.DELETE, state, null);
    }

    /**
     * 컬럼 생성 히스토리 기록
     */
    @Transactional
    public void recordColumnCreation(Integer userKey, Long projectKey, Long columnKey, Map<String, Object> state) {
        recordHistory(userKey, projectKey, columnKey, EditHistoryTargetType.COLUMN,
                     EditHistoryActionType.ADD, null, state);
    }

    /**
     * 컬럼 수정 히스토리 기록
     */
    @Transactional
    public void recordColumnUpdate(Integer userKey, Long projectKey, Long columnKey,
                                   Map<String, Object> beforeState, Map<String, Object> afterState) {
        recordHistory(userKey, projectKey, columnKey, EditHistoryTargetType.COLUMN,
                     EditHistoryActionType.UPDATE, beforeState, afterState);
    }

    /**
     * 컬럼 삭제 히스토리 기록
     */
    @Transactional
    public void recordColumnDeletion(Integer userKey, Long projectKey, Long columnKey, Map<String, Object> state) {
        recordHistory(userKey, projectKey, columnKey, EditHistoryTargetType.COLUMN,
                     EditHistoryActionType.DELETE, state, null);
    }

    /**
     * 관계 생성 히스토리 기록
     */
    @Transactional
    public void recordRelationCreation(Integer userKey, Long projectKey, Long relationKey, Map<String, Object> state) {
        recordHistory(userKey, projectKey, relationKey, EditHistoryTargetType.RELATION,
                     EditHistoryActionType.ADD, null, state);
    }

    /**
     * 관계 수정 히스토리 기록
     */
    @Transactional
    public void recordRelationUpdate(Integer userKey, Long projectKey, Long relationKey,
                                    Map<String, Object> beforeState, Map<String, Object> afterState) {
        recordHistory(userKey, projectKey, relationKey, EditHistoryTargetType.RELATION,
                     EditHistoryActionType.UPDATE, beforeState, afterState);
    }

    /**
     * 관계 삭제 히스토리 기록
     */
    @Transactional
    public void recordRelationDeletion(Integer userKey, Long projectKey, Long relationKey, Map<String, Object> state) {
        recordHistory(userKey, projectKey, relationKey, EditHistoryTargetType.RELATION,
                     EditHistoryActionType.DELETE, state, null);
    }
}
