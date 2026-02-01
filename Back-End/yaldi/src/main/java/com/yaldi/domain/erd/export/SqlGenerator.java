package com.yaldi.domain.erd.export;

import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdRelation;
import com.yaldi.domain.erd.entity.ErdTable;

import java.util.List;

/**
 * SQL DDL 생성 인터페이스
 */
public interface SqlGenerator {

    /**
     * CREATE TABLE 문 생성
     */
    String generateCreateTable(ErdTable table, List<ErdColumn> columns);

    /**
     * ALTER TABLE ADD FOREIGN KEY 문 생성
     */
    String generateForeignKey(ErdRelation relation, ErdTable fromTable, ErdColumn fromColumn, ErdTable toTable, ErdColumn toColumn);

    /**
     * 데이터 타입을 해당 SQL dialect에 맞게 변환
     */
    String convertDataType(String dataType, String[] dataDetail);
}
