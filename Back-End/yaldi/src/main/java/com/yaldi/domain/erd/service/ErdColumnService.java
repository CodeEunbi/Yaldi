package com.yaldi.domain.erd.service;

import com.yaldi.domain.erd.dto.request.ErdColumnCreateRequest;
import com.yaldi.domain.erd.dto.response.ErdColumnResponse;
import com.yaldi.domain.erd.dto.request.ErdColumnUpdateRequest;
import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.repository.ErdColumnRepository;
import com.yaldi.domain.edithistory.service.EditHistoryService;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERD 컬럼 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErdColumnService {

    private final ErdColumnRepository erdColumnRepository;
    private final ErdTableService erdTableService;
    private final ErdLockService erdLockService;
    private final EditHistoryService editHistoryService;

    /**
     * 테이블의 컬럼 목록 조회
     */
    public List<ErdColumnResponse> getColumnsByTableKey(Long tableKey) {
        return erdColumnRepository.findByTableKey(tableKey).stream()
                .map(ErdColumnResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 프로젝트의 모든 컬럼 조회 (1+N 쿼리 방지)
     */
    public List<ErdColumnResponse> getColumnsByProjectKey(Long projectKey) {
        return erdColumnRepository.findByProjectKey(projectKey).stream()
                .map(ErdColumnResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ERD 컬럼 단건 조회
     */
    public ErdColumnResponse getColumnById(Long columnKey) {
        ErdColumn column = erdColumnRepository.findById(columnKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_COLUMN_NOT_FOUND));
        return ErdColumnResponse.from(column);
    }

    /**
     * ERD 컬럼 생성
     */
    @Transactional
    public ErdColumnResponse createColumn(Long tableKey, ErdColumnCreateRequest request, Integer userKey) {
        // 컬럼 생성은 중요한 작업이므로 Lock 검증 필요
        erdLockService.validateTableLock(tableKey);

        ErdColumn column = ErdColumn.builder()
                .tableKey(tableKey)
                .isPrimaryKey(request.getIsPrimaryKey())
                .isForeignKey(request.getIsForeignKey())
                .build();

        ErdColumn savedColumn = erdColumnRepository.save(column);
        log.info("Created ERD column: {}", savedColumn.getColumnKey());

        // 히스토리 기록
        if (userKey != null) {
            Long projectKey = erdTableService.getProjectKeyByTableKey(tableKey);
            editHistoryService.recordColumnCreation(userKey, projectKey, savedColumn.getColumnKey(),
                    convertColumnToMap(savedColumn));
        }

        return ErdColumnResponse.from(savedColumn);
    }

    /**
     * ERD 컬럼 수정
     */
    @Transactional
    public ErdColumnResponse updateColumn(Long columnKey, ErdColumnUpdateRequest request, Integer userKey) {
        ErdColumn column = erdColumnRepository.findById(columnKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_COLUMN_NOT_FOUND));

        // 수정 전 상태 저장 (히스토리용)
        Map<String, Object> beforeState = convertColumnToMap(column);

        // 중요한 컬럼 속성 변경 시에만 Lock 검증
        boolean isImportantUpdate = request.getLogicalName() != null ||
                request.getPhysicalName() != null ||
                request.getDataType() != null ||
                request.getIsPrimaryKey() != null ||
                request.getIsForeignKey() != null;

        if (isImportantUpdate) {
            erdLockService.validateTableLock(column.getTableKey());
        }

        if (request.getLogicalName() != null) {
            column.updateLogicalName(request.getLogicalName());
        }
        if (request.getPhysicalName() != null) {
            column.updatePhysicalName(request.getPhysicalName());
        }
        if (request.getDataType() != null) {
            column.updateDataType(request.getDataType(), request.getDataDetail());
        }
        if (request.getIsNullable() != null || request.getIsPrimaryKey() != null ||
                request.getIsForeignKey() != null || request.getIsUnique() != null ||
                request.getIsIncremental() != null) {
            column.updateConstraints(
                    request.getIsNullable() != null ? request.getIsNullable() : column.getIsNullable(),
                    request.getIsPrimaryKey() != null ? request.getIsPrimaryKey() : column.getIsPrimaryKey(),
                    request.getIsForeignKey() != null ? request.getIsForeignKey() : column.getIsForeignKey(),
                    request.getIsUnique() != null ? request.getIsUnique() : column.getIsUnique(),
                    request.getIsIncremental() != null ? request.getIsIncremental() : column.getIsIncremental()
            );
        }
        if (request.getDefaultValue() != null) {
            column.updateDefaultValue(request.getDefaultValue());
        }
        if (request.getComment() != null) {
            column.updateComment(request.getComment());
        }
        if (request.getColumnOrder() != null) {
            column.updateColumnOrder(request.getColumnOrder());
        }

        log.info("Updated ERD column: {}", columnKey);

        // 히스토리 기록
        if (userKey != null) {
            Long projectKey = erdTableService.getProjectKeyByTableKey(column.getTableKey());
            Map<String, Object> afterState = convertColumnToMap(column);
            editHistoryService.recordColumnUpdate(userKey, projectKey, columnKey, beforeState, afterState);
        }

        return ErdColumnResponse.from(column);
    }

    /**
     * ERD 컬럼 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteColumn(Long columnKey, Integer userKey) {
        ErdColumn column = erdColumnRepository.findById(columnKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_COLUMN_NOT_FOUND));

        // 삭제 전 상태 저장 (히스토리용)
        Map<String, Object> beforeState = convertColumnToMap(column);

        // 컬럼 삭제는 중요한 작업이므로 Lock 검증 필요
        erdLockService.validateTableLock(column.getTableKey());

        column.softDelete();
        log.info("Deleted ERD column: {}", columnKey);

        // 히스토리 기록
        if (userKey != null) {
            Long projectKey = erdTableService.getProjectKeyByTableKey(column.getTableKey());
            editHistoryService.recordColumnDeletion(userKey, projectKey, columnKey, beforeState);
        }
    }

    /**
     * 컬럼 순서 변경 (WebSocket용)
     */
    @Transactional
    public void updateColumnOrder(Long columnKey, Integer columnOrder) {
        ErdColumn column = erdColumnRepository.findById(columnKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_COLUMN_NOT_FOUND));
        column.updateColumnOrder(columnOrder);
        log.info("Updated column order: columnKey={}, order={}", columnKey, columnOrder);
    }

    /**
     * 컬럼이 속한 테이블의 프로젝트 키 조회
     */
    public Long getProjectKeyByColumnKey(Long columnKey) {
        ErdColumn column = erdColumnRepository.findById(columnKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_COLUMN_NOT_FOUND));
        return erdTableService.getProjectKeyByTableKey(column.getTableKey());
    }

    /**
     * ErdColumn을 Map으로 변환 (히스토리 기록용)
     */
    private Map<String, Object> convertColumnToMap(ErdColumn column) {
        Map<String, Object> map = new HashMap<>();
        map.put("columnKey", column.getColumnKey());
        map.put("tableKey", column.getTableKey());
        map.put("logicalName", column.getLogicalName());
        map.put("physicalName", column.getPhysicalName());
        map.put("dataType", column.getDataType());
        if (column.getDataDetail() != null) {
            map.put("dataDetail", Arrays.asList(column.getDataDetail()));
        }
        map.put("isNullable", column.getIsNullable());
        map.put("isPrimaryKey", column.getIsPrimaryKey());
        map.put("isForeignKey", column.getIsForeignKey());
        map.put("isUnique", column.getIsUnique());
        map.put("isIncremental", column.getIsIncremental());
        map.put("defaultValue", column.getDefaultValue());
        map.put("comment", column.getComment());
        map.put("columnOrder", column.getColumnOrder());
        return map;
    }
}
