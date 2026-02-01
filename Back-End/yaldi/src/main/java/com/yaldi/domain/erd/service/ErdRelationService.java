package com.yaldi.domain.erd.service;

import com.yaldi.domain.erd.dto.request.ErdRelationCreateRequest;
import com.yaldi.domain.erd.dto.response.ErdColumnResponse;
import com.yaldi.domain.erd.dto.response.ErdRelationResponse;
import com.yaldi.domain.erd.dto.request.ErdRelationUpdateRequest;
import com.yaldi.domain.erd.dto.response.ErdRelationWithFkResponse;
import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdRelation;
import com.yaldi.domain.erd.repository.ErdColumnRepository;
import com.yaldi.domain.erd.repository.ErdRelationRepository;
import com.yaldi.domain.edithistory.service.EditHistoryService;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERD 관계 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErdRelationService {

    private final ErdRelationRepository erdRelationRepository;
    private final EditHistoryService editHistoryService;
    private final ErdColumnRepository erdColumnRepository;

    /**
     * 프로젝트의 ERD 관계 목록 조회
     */
    public List<ErdRelationResponse> getRelationsByProjectKey(Long projectKey) {
        return erdRelationRepository.findByProjectKey(projectKey).stream()
                .map(ErdRelationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ERD 관계 단건 조회
     */
    public ErdRelationResponse getRelationById(Long relationKey) {
        ErdRelation relation = erdRelationRepository.findById(relationKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_RELATION_NOT_FOUND));
        return ErdRelationResponse.from(relation);
    }

    /**
     * ERD 관계 생성
     */
    @Transactional
    public ErdRelationWithFkResponse createRelation(Long projectKey, ErdRelationCreateRequest request, Integer userKey) {
        ErdRelation relation = ErdRelation.builder()
                .projectKey(projectKey)
                .fromTableKey(request.getFromTableKey())
                .fromColumnKey(request.getFromColumnKey())
                .toTableKey(request.getToTableKey())
                .relationType(request.getRelationType())
                .constraintName(request.getConstraintName())
                .onDeleteAction(request.getOnDeleteAction())
                .onUpdateAction(request.getOnUpdateAction())
                .build();

        ErdRelation savedRelation = erdRelationRepository.save(relation);
        log.info("Created ERD relation: {}", savedRelation.getRelationKey());

        List<ErdColumn> pks = erdColumnRepository.findByTableKeyAndIsPrimaryKeyTrue(
                request.getFromTableKey());

        log.info("pk size: {}", pks.size());

        List<ErdColumnResponse> columns = new ArrayList<>();
        Long toColumnKey = null;
        for(ErdColumn pk : pks) {
            ErdColumn newColumn = ErdColumn.builder()
                    .tableKey(request.getToTableKey())
                    .isForeignKey(true)
                    .isIncremental(false)
                    .isNullable(pk.getIsNullable())
                    .isPrimaryKey(false)
                    .logicalName(pk.getLogicalName())
                    .dataDetail(pk.getDataDetail())
                    .physicalName(pk.getPhysicalName())
                    .comment(pk.getComment())
                    .dataType(pk.getDataType())
                    .isUnique(pk.getIsUnique())
                    .build();
            ErdColumn savedColumn = erdColumnRepository.save(newColumn);
            toColumnKey = savedColumn.getColumnKey();
            columns.add(ErdColumnResponse.from(savedColumn));
        }

        // relation에 toColumnKey 설정
        savedRelation.updateColumns(savedRelation.getFromColumnKey(),
                toColumnKey);

        // 히스토리 기록
        if (userKey != null) {
            editHistoryService.recordRelationCreation(userKey, projectKey, savedRelation.getRelationKey(),
                    convertRelationToMap(savedRelation));
        }

        return ErdRelationWithFkResponse.builder()
                .erdRelationResponse(ErdRelationResponse.from(savedRelation))
                .columns(columns)
                .build();
    }

    /**
     * ERD 관계 수정
     */
    @Transactional
    public ErdRelationResponse updateRelation(Long relationKey, ErdRelationUpdateRequest request, Integer userKey) {
        ErdRelation relation = erdRelationRepository.findById(relationKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_RELATION_NOT_FOUND));

        // 수정 전 상태 저장 (히스토리용)
        Map<String, Object> beforeState = convertRelationToMap(relation);

        if (request.getFromColumnKey() != null || request.getToColumnKey() != null) {
            relation.updateColumns(request.getFromColumnKey(), request.getToColumnKey());
        }
        if (request.getRelationType() != null) {
            relation.updateRelationType(request.getRelationType());
        }
        if (request.getConstraintName() != null) {
            relation.updateConstraintName(request.getConstraintName());
        }
        if (request.getOnDeleteAction() != null && request.getOnUpdateAction() != null) {
            relation.updateReferentialActions(request.getOnDeleteAction(), request.getOnUpdateAction());
        }

        log.info("Updated ERD relation: {}", relationKey);

        // 히스토리 기록
        if (userKey != null) {
            Map<String, Object> afterState = convertRelationToMap(relation);
            editHistoryService.recordRelationUpdate(userKey, relation.getProjectKey(), relationKey, beforeState, afterState);
        }

        return ErdRelationResponse.from(relation);
    }

    /**
     * ERD 관계 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteRelation(Long relationKey, Integer userKey) {
        ErdRelation relation = erdRelationRepository.findById(relationKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_RELATION_NOT_FOUND));

        // 삭제 전 상태 저장 (히스토리용)
        Map<String, Object> beforeState = convertRelationToMap(relation);

        relation.softDelete();
        log.info("Deleted ERD relation: {}", relationKey);

        // 히스토리 기록
        if (userKey != null) {
            editHistoryService.recordRelationDeletion(userKey, relation.getProjectKey(), relationKey, beforeState);
        }
    }

    /**
     * Relation 키로 Project 키 조회
     */
    public Long getProjectKeyByRelationKey(Long relationKey) {
        ErdRelation relation = erdRelationRepository.findById(relationKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_RELATION_NOT_FOUND));
        return relation.getProjectKey();
    }

    /**
     * ErdRelation을 Map으로 변환 (히스토리 기록용)
     */
    private Map<String, Object> convertRelationToMap(ErdRelation relation) {
        Map<String, Object> map = new HashMap<>();
        map.put("relationKey", relation.getRelationKey());
        map.put("projectKey", relation.getProjectKey());
        map.put("fromTableKey", relation.getFromTableKey());
        map.put("fromColumnKey", relation.getFromColumnKey());
        map.put("toTableKey", relation.getToTableKey());
        map.put("toColumnKey", relation.getToColumnKey());
        map.put("relationType", relation.getRelationType().getValue());
        map.put("constraintName", relation.getConstraintName());
        map.put("onDeleteAction", relation.getOnDeleteAction().getValue());
        map.put("onUpdateAction", relation.getOnUpdateAction().getValue());
        return map;
    }
}
