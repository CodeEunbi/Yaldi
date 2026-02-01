package com.yaldi.domain.erd.export;

import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdRelation;
import com.yaldi.domain.erd.entity.ErdTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL DDL 생성기
 */
@Component
public class PostgreSqlGenerator implements SqlGenerator {

    @Override
    public String generateCreateTable(ErdTable table, List<ErdColumn> columns) {
        StringBuilder sql = new StringBuilder();

        sql.append("CREATE TABLE ").append(escapeIdentifier(table.getPhysicalName())).append(" (\n");

        // 컬럼 정의
        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (ErdColumn column : columns) {
            StringBuilder colDef = new StringBuilder();
            colDef.append("  ").append(escapeIdentifier(column.getPhysicalName()));
            colDef.append(" ").append(convertDataType(column.getDataType(), column.getDataDetail()));

            // AUTO_INCREMENT (PostgreSQL에서는 SERIAL 또는 GENERATED)
            if (column.getIsIncremental()) {
                // 이미 SERIAL/BIGSERIAL로 변환되었으므로 별도 처리 불필요
                // 하지만 명시적으로 GENERATED를 사용하고 싶다면:
                // colDef.append(" GENERATED ALWAYS AS IDENTITY");
            }

            // NOT NULL
            if (!column.getIsNullable()) {
                colDef.append(" NOT NULL");
            }

            // UNIQUE
            if (column.getIsUnique() && !column.getIsPrimaryKey()) {
                colDef.append(" UNIQUE");
            }

            // DEFAULT
            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                colDef.append(" DEFAULT ").append(formatDefaultValue(column.getDefaultValue()));
            }

            columnDefinitions.add(colDef.toString());

            // PRIMARY KEY 수집
            if (column.getIsPrimaryKey()) {
                primaryKeys.add(escapeIdentifier(column.getPhysicalName()));
            }
        }

        sql.append(String.join(",\n", columnDefinitions));

        // PRIMARY KEY 제약조건
        if (!primaryKeys.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")");
        }

        sql.append("\n);\n");

        // 테이블 코멘트
        if (table.getLogicalName() != null && !table.getLogicalName().isEmpty()) {
            sql.append("\nCOMMENT ON TABLE ").append(escapeIdentifier(table.getPhysicalName()))
               .append(" IS '").append(escapeSingleQuote(table.getLogicalName())).append("';\n");
        }

        // 컬럼 코멘트
        for (ErdColumn column : columns) {
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                sql.append("COMMENT ON COLUMN ").append(escapeIdentifier(table.getPhysicalName()))
                   .append(".").append(escapeIdentifier(column.getPhysicalName()))
                   .append(" IS '").append(escapeSingleQuote(column.getComment())).append("';\n");
            } else if (column.getLogicalName() != null && !column.getLogicalName().isEmpty()) {
                sql.append("COMMENT ON COLUMN ").append(escapeIdentifier(table.getPhysicalName()))
                   .append(".").append(escapeIdentifier(column.getPhysicalName()))
                   .append(" IS '").append(escapeSingleQuote(column.getLogicalName())).append("';\n");
            }
        }

        return sql.toString();
    }

    @Override
    public String generateForeignKey(ErdRelation relation, ErdTable fromTable, ErdColumn fromColumn,
                                     ErdTable toTable, ErdColumn toColumn) {
        StringBuilder sql = new StringBuilder();

        // 제약조건 이름 생성 규칙:
        // 1. 사용자 지정 이름이 있으면 사용
        // 2. 없으면 자동 생성: fk_{from_table}_{from_column}
        // 3. 길이 제한: PostgreSQL은 63자, MySQL은 64자
        String constraintName = relation.getConstraintName();
        if (constraintName == null || constraintName.trim().isEmpty()) {
            constraintName = generateForeignKeyName(fromTable.getPhysicalName(), fromColumn.getPhysicalName());
        }
        // PostgreSQL identifier 최대 길이는 63자
        if (constraintName.length() > 63) {
            constraintName = constraintName.substring(0, 63);
        }

        sql.append("ALTER TABLE ").append(escapeIdentifier(fromTable.getPhysicalName()));
        sql.append("\n  ADD CONSTRAINT ").append(escapeIdentifier(constraintName));
        sql.append("\n  FOREIGN KEY (").append(escapeIdentifier(fromColumn.getPhysicalName())).append(")");
        sql.append("\n  REFERENCES ").append(escapeIdentifier(toTable.getPhysicalName()));
        sql.append(" (").append(escapeIdentifier(toColumn.getPhysicalName())).append(")");

        // ON DELETE
        if (relation.getOnDeleteAction() != null) {
            sql.append("\n  ON DELETE ").append(relation.getOnDeleteAction().getValue());
        }

        // ON UPDATE
        if (relation.getOnUpdateAction() != null) {
            sql.append("\n  ON UPDATE ").append(relation.getOnUpdateAction().getValue());
        }

        sql.append(";\n");

        return sql.toString();
    }

    @Override
    public String convertDataType(String dataType, String[] dataDetail) {
        if (dataType == null || dataType.trim().isEmpty()) {
            return "TEXT"; // 기본값: TEXT
        }

        String upperType = dataType.trim().toUpperCase();

        // 기본 타입 매핑
        switch (upperType) {
            // 정수 타입
            case "INT":
            case "INTEGER":
                return "INTEGER";
            case "BIGINT":
                return "BIGINT";
            case "SMALLINT":
                return "SMALLINT";
            case "TINYINT":
                return "SMALLINT"; // PostgreSQL에는 TINYINT가 없음 → SMALLINT로 변환

            // 부동소수점
            case "FLOAT":
                return "REAL";
            case "DOUBLE":
                return "DOUBLE PRECISION";
            case "DECIMAL":
            case "NUMERIC":
                if (dataDetail != null && dataDetail.length >= 2) {
                    return "NUMERIC(" + dataDetail[0] + ", " + dataDetail[1] + ")";
                } else if (dataDetail != null && dataDetail.length == 1) {
                    return "NUMERIC(" + dataDetail[0] + ")";
                }
                return "NUMERIC";

            // 문자열
            case "CHAR":
                if (dataDetail != null && dataDetail.length > 0) {
                    return "CHAR(" + dataDetail[0] + ")";
                }
                return "CHAR(1)";
            case "VARCHAR":
                if (dataDetail != null && dataDetail.length > 0) {
                    return "VARCHAR(" + dataDetail[0] + ")";
                }
                return "VARCHAR(255)";
            case "TEXT":
            case "MEDIUMTEXT":
            case "LONGTEXT":
                return "TEXT";

            // 날짜/시간
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIME";
            case "DATETIME":
            case "TIMESTAMP":
                return "TIMESTAMP";

            // Boolean
            case "BOOLEAN":
            case "BOOL":
                return "BOOLEAN";

            // JSON
            case "JSON":
                return "JSON";
            case "JSONB":
                return "JSONB";

            // Binary
            case "BLOB":
            case "BYTEA":
                return "BYTEA";

            // UUID
            case "UUID":
                return "UUID";

            // 배열 타입 (PostgreSQL 특화)
            case "TEXT[]":
            case "VARCHAR[]":
                return "TEXT[]";
            case "INT[]":
            case "INTEGER[]":
                return "INTEGER[]";
            case "BIGINT[]":
                return "BIGINT[]";

            default:
                // 배열 타입 패턴 확인 ([]로 끝나는 경우)
                if (upperType.endsWith("[]")) {
                    return dataType; // PostgreSQL은 배열을 그대로 지원
                }
                // 알 수 없는 타입은 그대로 반환
                return dataType;
        }
    }

    /**
     * 외래키 제약조건 이름 자동 생성
     */
    private String generateForeignKeyName(String fromTable, String fromColumn) {
        // 기본 형식: fk_{from_table}_{from_column}
        String name = "fk_" + fromTable + "_" + fromColumn;
        // 특수문자 제거 (identifier로 사용 가능한 문자만)
        name = name.replaceAll("[^a-zA-Z0-9_]", "_");
        return name;
    }

    /**
     * 식별자를 큰따옴표로 감싸기 (PostgreSQL)
     */
    private String escapeIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "\"\"";
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * 작은따옴표 이스케이프
     */
    private String escapeSingleQuote(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    /**
     * DEFAULT 값 포맷팅
     */
    private String formatDefaultValue(String value) {
        if (value == null || value.isEmpty()) {
            return "NULL";
        }

        // 숫자, boolean, NULL, 함수 등은 그대로
        if (value.matches("^-?\\d+(\\.\\d+)?$") ||
            value.equalsIgnoreCase("true") ||
            value.equalsIgnoreCase("false") ||
            value.equalsIgnoreCase("null") ||
            value.toUpperCase().contains("CURRENT_") ||
            value.contains("()")) {
            return value;
        }

        // 문자열은 작은따옴표로 감싸기
        return "'" + escapeSingleQuote(value) + "'";
    }
}
