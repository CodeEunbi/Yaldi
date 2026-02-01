package com.yaldi.domain.version.service;

import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdRelation;
import com.yaldi.domain.erd.entity.ErdTable;
import com.yaldi.domain.erd.repository.ErdColumnRepository;
import com.yaldi.domain.erd.repository.ErdRelationRepository;
import com.yaldi.domain.erd.repository.ErdTableRepository;
import com.yaldi.domain.version.util.SchemaDataConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yaldi.domain.version.util.SchemaDataConverter.*;

/**
 * 버전 롤백 기능을 담당하는 서비스
 * - ERD 객체 삭제
 * - 스냅샷 데이터로부터 ERD 재생성 (테이블, 컬럼, 관계)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionRollbackService {

    private final ErdTableRepository erdTableRepository;
    private final ErdColumnRepository erdColumnRepository;
    private final ErdRelationRepository erdRelationRepository;

    /**
     * 특정 버전의 스냅샷 데이터를 기반으로 ERD를 롤백
     *
     * @param projectKey 프로젝트 키
     * @param schemaData 버전의 스냅샷 데이터 (JSON)
     */
    @Transactional
    public void rollbackErdToSnapshot(Long projectKey, Map<String, Object> schemaData) {
        List<Map<String, Object>> tables = safeList(schemaData.get("tables"));
        List<Map<String, Object>> relations = safeList(schemaData.get("relations"));

        // 1) 기존 ERD 완전 삭제
        deleteAllErdObjects(projectKey);

        // 2) 테이블 & 컬럼 생성
        Map<Long, Long> tableKeyMap = new HashMap<>();
        Map<Long, Long> columnKeyMap = new HashMap<>();

        createTablesAndColumns(projectKey, tables, tableKeyMap, columnKeyMap);

        // 3) 관계 재생성
        createRelations(projectKey, relations, tableKeyMap, columnKeyMap);

        log.info("ERD 롤백 완료 - projectKey={}, 테이블 수={}, 관계 수={}",
                projectKey, tables.size(), relations.size());
    }

    /**
     * 프로젝트의 모든 ERD 객체 삭제 (관계 → 컬럼 → 테이블 순서)
     */
    private void deleteAllErdObjects(Long projectKey) {
        erdRelationRepository.deleteAll(erdRelationRepository.findByProjectKey(projectKey));
        erdColumnRepository.deleteAll(erdColumnRepository.findByProjectKey(projectKey));
        erdTableRepository.deleteAll(erdTableRepository.findByProjectKey(projectKey));
        log.debug("기존 ERD 객체 삭제 완료 - projectKey={}", projectKey);
    }

    /**
     * 스냅샷 데이터로부터 테이블과 컬럼 생성
     *
     * @param projectKey    프로젝트 키
     * @param tables        스냅샷의 테이블 목록
     * @param tableKeyMap   이전 테이블 키 → 새 테이블 키 매핑
     * @param columnKeyMap  이전 컬럼 키 → 새 컬럼 키 매핑
     */
    private void createTablesAndColumns(
            Long projectKey,
            List<Map<String, Object>> tables,
            Map<Long, Long> tableKeyMap,
            Map<Long, Long> columnKeyMap
    ) {
        for (Map<String, Object> tableData : tables) {
            Long oldTableKey = toLong(tableData.get("tableKey"));

            // 테이블 생성
            ErdTable newTable = createTable(projectKey, tableData);
            tableKeyMap.put(oldTableKey, newTable.getTableKey());

            // 컬럼 생성
            List<Map<String, Object>> columns = safeList(tableData.get("columns"));
            createColumns(newTable.getTableKey(), columns, columnKeyMap);
        }
        log.debug("테이블 및 컬럼 생성 완료 - 테이블 수={}", tables.size());
    }

    /**
     * 단일 테이블 생성
     */
    private ErdTable createTable(Long projectKey, Map<String, Object> tableData) {
        return erdTableRepository.save(
                ErdTable.builder()
                        .projectKey(projectKey)
                        .logicalName(toStringSafe(tableData.get("logicalName"), ""))
                        .physicalName(toStringSafe(tableData.get("physicalName"), ""))
                        .xPosition(toBigDecimal(tableData.get("xPosition"), BigDecimal.ZERO))
                        .yPosition(toBigDecimal(tableData.get("yPosition"), BigDecimal.ZERO))
                        .colorHex(toStringSafe(tableData.get("colorHex"), null))
                        .build()
        );
    }

    /**
     * 테이블의 컬럼들 생성
     */
    private void createColumns(
            Long newTableKey,
            List<Map<String, Object>> columns,
            Map<Long, Long> columnKeyMap
    ) {
        for (Map<String, Object> columnData : columns) {
            Long oldColumnKey = toLong(columnData.get("columnKey"));

            ErdColumn newColumn = erdColumnRepository.save(
                    ErdColumn.builder()
                            .tableKey(newTableKey)
                            .logicalName(toStringSafe(columnData.get("logicalName"), ""))
                            .physicalName(toStringSafe(columnData.get("physicalName"), ""))
                            .dataType(toStringSafe(columnData.get("dataType"), "VARCHAR"))
                            .dataDetail(toArray(columnData.get("dataDetail")))
                            .isNullable(toBoolean(columnData.get("isNullable"), true))
                            .isPrimaryKey(toBoolean(columnData.get("isPrimaryKey"), false))
                            .isForeignKey(toBoolean(columnData.get("isForeignKey"), false))
                            .isUnique(toBoolean(columnData.get("isUnique"), false))
                            .isIncremental(toBoolean(columnData.get("isIncremental"), false))
                            .defaultValue(toStringSafe(columnData.get("defaultValue"), null))
                            .comment(toStringSafe(columnData.get("comment"), null))
                            .columnOrder(toInteger(columnData.get("columnOrder"), 0))
                            .build()
            );

            columnKeyMap.put(oldColumnKey, newColumn.getColumnKey());
        }
    }

    /**
     * 스냅샷 데이터로부터 관계 생성
     *
     * @param projectKey    프로젝트 키
     * @param relations     스냅샷의 관계 목록
     * @param tableKeyMap   이전 테이블 키 → 새 테이블 키 매핑
     * @param columnKeyMap  이전 컬럼 키 → 새 컬럼 키 매핑
     */
    private void createRelations(
            Long projectKey,
            List<Map<String, Object>> relations,
            Map<Long, Long> tableKeyMap,
            Map<Long, Long> columnKeyMap
    ) {
        for (Map<String, Object> relationData : relations) {
            Long oldFromTableKey = toLong(relationData.get("fromTableKey"));
            Long oldToTableKey = toLong(relationData.get("toTableKey"));
            Long oldFromColumnKey = toLongOrNull(relationData.get("fromColumnKey"));
            Long oldToColumnKey = toLongOrNull(relationData.get("toColumnKey"));

            erdRelationRepository.save(
                    ErdRelation.builder()
                            .projectKey(projectKey)
                            .fromTableKey(tableKeyMap.get(oldFromTableKey))
                            .toTableKey(tableKeyMap.get(oldToTableKey))
                            .fromColumnKey(oldFromColumnKey == null ? null : columnKeyMap.get(oldFromColumnKey))
                            .toColumnKey(oldToColumnKey == null ? null : columnKeyMap.get(oldToColumnKey))
                            .relationType(toRelationType(relationData.get("relationType")))
                            .constraintName(toStringSafe(relationData.get("constraintName"), ""))
                            .onDeleteAction(toReferentialActionType(relationData.get("onDeleteAction")))
                            .onUpdateAction(toReferentialActionType(relationData.get("onUpdateAction")))
                            .build()
            );
        }
        log.debug("관계 생성 완료 - 관계 수={}", relations.size());
    }
}
