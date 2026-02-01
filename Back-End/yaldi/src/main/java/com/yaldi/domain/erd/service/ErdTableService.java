package com.yaldi.domain.erd.service;

import com.yaldi.domain.erd.dto.request.ErdTableCreateRequest;
import com.yaldi.domain.erd.dto.response.ErdTableResponse;
import com.yaldi.domain.erd.dto.request.ErdTableUpdateRequest;
import com.yaldi.domain.erd.entity.ErdTable;
import com.yaldi.domain.erd.repository.ErdTableRepository;
import com.yaldi.domain.edithistory.service.EditHistoryService;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERD 테이블 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErdTableService {

    private final ErdTableRepository erdTableRepository;
    private final ErdLockService erdLockService;
    private final EditHistoryService editHistoryService;

    /**
     * 프로젝트의 ERD 테이블 목록 조회
     */
    public List<ErdTableResponse> getTablesByProjectKey(Long projectKey) {
        return erdTableRepository.findByProjectKey(projectKey).stream()
                .map(ErdTableResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ERD 테이블 단건 조회
     */
    public ErdTableResponse getTableById(Long tableKey) {
        ErdTable table = erdTableRepository.findById(tableKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_TABLE_NOT_FOUND));
        return ErdTableResponse.from(table);
    }

    /**
     * ERD 테이블 생성
     */
    @Transactional
    public ErdTableResponse createTable(Long projectKey, ErdTableCreateRequest request, Integer userKey) {
        ErdTable table = ErdTable.builder()
                .projectKey(projectKey)
                .logicalName(request.getLogicalName())
                .physicalName(request.getPhysicalName())
                .xPosition(request.getXPosition())
                .yPosition(request.getYPosition())
                .colorHex(request.getColorHex())
                .build();

        ErdTable savedTable = erdTableRepository.save(table);
        log.info("Created ERD table: {}", savedTable.getTableKey());

        // 히스토리 기록
        if (userKey != null) {
            editHistoryService.recordTableCreation(userKey, projectKey, savedTable.getTableKey(),
                    convertTableToMap(savedTable));
        }

        return ErdTableResponse.from(savedTable);
    }

    /**
     * ERD 테이블 수정
     * Lock이 필요한 중요한 수정(이름, 물리명)은 Lock 검증 수행
     */
    @Transactional
    public ErdTableResponse updateTable(Long tableKey, ErdTableUpdateRequest request, Integer userKey) {
        ErdTable table = erdTableRepository.findById(tableKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_TABLE_NOT_FOUND));

        // 수정 전 상태 저장 (히스토리용)
        Map<String, Object> beforeState = convertTableToMap(table);

        // 중요한 수정 (이름/물리명)인 경우 Lock 검증 필요
        // 색상/위치는 실시간 변경이므로 Lock 없이도 수정 가능
        boolean isImportantUpdate = request.getLogicalName() != null || request.getPhysicalName() != null;

        if (isImportantUpdate) {
            erdLockService.validateTableLock(tableKey);
        }

        if (request.getLogicalName() != null) {
            table.updateLogicalName(request.getLogicalName());
        }
        if (request.getPhysicalName() != null) {
            table.updatePhysicalName(request.getPhysicalName());
        }
        if (request.getXPosition() != null && request.getYPosition() != null) {
            table.updatePosition(request.getXPosition(), request.getYPosition());
        }
        if (request.getColorHex() != null) {
            table.updateColorHex(request.getColorHex());
        }

        log.info("Updated ERD table: {}", tableKey);

        // 히스토리 기록
        if (userKey != null) {
            Map<String, Object> afterState = convertTableToMap(table);
            editHistoryService.recordTableUpdate(userKey, table.getProjectKey(), tableKey, beforeState, afterState);
        }

        return ErdTableResponse.from(table);
    }

    /**
     * ERD 테이블 위치 업데이트 (실시간 협업용)
     */
    @Transactional
    public void updatePosition(Long tableKey, BigDecimal xPosition, BigDecimal yPosition) {
        ErdTable table = erdTableRepository.findById(tableKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_TABLE_NOT_FOUND));
        table.updatePosition(xPosition, yPosition);
    }

    /**
     * 테이블 키로 프로젝트 키 조회 (WebSocket용)
     */
    public Long getProjectKeyByTableKey(Long tableKey) {
        ErdTable table = erdTableRepository.findById(tableKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_TABLE_NOT_FOUND));
        return table.getProjectKey();
    }

    /**
     * ERD 테이블 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteTable(Long tableKey, Integer userKey) {
        ErdTable table = erdTableRepository.findById(tableKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.ERD_TABLE_NOT_FOUND));

        // 삭제 전 상태 저장 (히스토리용)
        Map<String, Object> beforeState = convertTableToMap(table);

        // 테이블 삭제는 중요한 작업이므로 Lock 검증 필요
        erdLockService.validateTableLock(tableKey);

        erdLockService.unlockTable(tableKey, userKey.toString());
        // 히스토리 기록
        if (userKey != null) {
            editHistoryService.recordTableDeletion(userKey, table.getProjectKey(), tableKey, beforeState);
        }
        table.softDelete();
        log.info("Deleted ERD table: {}", tableKey);

    }

    /**
     * ErdTable을 Map으로 변환 (히스토리 기록용)
     */
    private Map<String, Object> convertTableToMap(ErdTable table) {
        Map<String, Object> map = new HashMap<>();
        map.put("tableKey", table.getTableKey());
        map.put("projectKey", table.getProjectKey());
        map.put("logicalName", table.getLogicalName());
        map.put("physicalName", table.getPhysicalName());
        map.put("xPosition", table.getXPosition());
        map.put("yPosition", table.getYPosition());
        map.put("colorHex", table.getColorHex());
        return map;
    }
}
