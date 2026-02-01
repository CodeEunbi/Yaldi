package com.yaldi.domain.erd.service;

import com.yaldi.domain.erd.entity.*;
import com.yaldi.domain.erd.export.SqlGenerator;
import com.yaldi.domain.erd.export.SqlGeneratorFactory;
import com.yaldi.domain.erd.repository.ErdColumnRepository;
import com.yaldi.domain.erd.repository.ErdRelationRepository;
import com.yaldi.domain.erd.repository.ErdTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERD SQL Export 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErdExportService {

    private final ErdTableRepository erdTableRepository;
    private final ErdColumnRepository erdColumnRepository;
    private final ErdRelationRepository erdRelationRepository;
    private final SqlGeneratorFactory sqlGeneratorFactory;

    /**
     * 프로젝트의 ERD를 SQL DDL로 Export
     *
     * @param projectKey 프로젝트 키
     * @param dialect SQL Dialect (POSTGRESQL, MYSQL)
     * @return SQL DDL 문자열
     */
    public String exportToSql(Long projectKey, SqlDialect dialect) {
        log.info("Exporting ERD to SQL - projectKey: {}, dialect: {}", projectKey, dialect);

        // SQL Generator 선택
        SqlGenerator generator = sqlGeneratorFactory.getGenerator(dialect);

        StringBuilder sql = new StringBuilder();

        // 헤더 코멘트
        sql.append("-- ").append(dialect.getValue()).append(" DDL Export\n");
        sql.append("-- Project Key: ").append(projectKey).append("\n");
        sql.append("-- Generated at: ").append(java.time.LocalDateTime.now()).append("\n\n");

        // 1. 모든 테이블과 컬럼 조회
        List<ErdTable> tables = erdTableRepository.findByProjectKey(projectKey);
        List<ErdColumn> allColumns = erdColumnRepository.findByProjectKey(projectKey);

        // 테이블별로 컬럼 그룹핑
        Map<Long, List<ErdColumn>> columnsByTable = allColumns.stream()
                .collect(Collectors.groupingBy(ErdColumn::getTableKey));

        // 2. CREATE TABLE 문 생성
        for (ErdTable table : tables) {
            List<ErdColumn> columns = columnsByTable.getOrDefault(table.getTableKey(), List.of());

            // 컬럼 순서대로 정렬
            columns = columns.stream()
                    .sorted((c1, c2) -> Integer.compare(c1.getColumnOrder(), c2.getColumnOrder()))
                    .collect(Collectors.toList());

            String createTableSql = generator.generateCreateTable(table, columns);
            sql.append(createTableSql).append("\n");
        }

        // 3. 외래키 제약조건 생성
        List<ErdRelation> relations = erdRelationRepository.findByProjectKey(projectKey);

        if (!relations.isEmpty()) {
            sql.append("-- Foreign Key Constraints\n");

            // 테이블과 컬럼을 빠르게 조회하기 위한 Map 생성
            Map<Long, ErdTable> tableMap = tables.stream()
                    .collect(Collectors.toMap(ErdTable::getTableKey, t -> t));
            Map<Long, ErdColumn> columnMap = allColumns.stream()
                    .collect(Collectors.toMap(ErdColumn::getColumnKey, c -> c));

            for (ErdRelation relation : relations) {
                ErdTable fromTable = tableMap.get(relation.getFromTableKey());
                ErdTable toTable = tableMap.get(relation.getToTableKey());
                ErdColumn fromColumn = columnMap.get(relation.getFromColumnKey());
                ErdColumn toColumn = columnMap.get(relation.getToColumnKey());

                if (fromTable != null && toTable != null && fromColumn != null && toColumn != null) {
                    String fkSql = generator.generateForeignKey(relation, fromTable, fromColumn, toTable, toColumn);
                    sql.append(fkSql).append("\n");
                } else {
                    log.warn("Skipping relation {} due to missing table or column", relation.getRelationKey());
                }
            }
        }

        log.info("SQL Export completed - projectKey: {}, dialect: {}", projectKey, dialect);
        return sql.toString();
    }
}
