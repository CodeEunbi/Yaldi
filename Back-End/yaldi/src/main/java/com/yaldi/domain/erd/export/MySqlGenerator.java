package com.yaldi.domain.erd.export;

import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdRelation;
import com.yaldi.domain.erd.entity.ErdTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQL DDL 생성기
 */
@Component
public class MySqlGenerator implements SqlGenerator {

    @Override
    public String generateCreateTable(ErdTable table, List<ErdColumn> columns) {
        StringBuilder sql = new StringBuilder();

        sql.append("CREATE TABLE `").append(table.getPhysicalName()).append("` (\n");

        // 컬럼 정의
        List<String> columnDefinitions = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        for (ErdColumn column : columns) {
            StringBuilder colDef = new StringBuilder();
            colDef.append("  `").append(column.getPhysicalName()).append("`");
            colDef.append(" ").append(convertDataType(column.getDataType(), column.getDataDetail()));

            // NOT NULL
            if (!column.getIsNullable()) {
                colDef.append(" NOT NULL");
            }

            // AUTO_INCREMENT
            if (column.getIsIncremental()) {
                colDef.append(" AUTO_INCREMENT");
            }

            // DEFAULT
            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                colDef.append(" DEFAULT ").append(formatDefaultValue(column.getDefaultValue()));
            }

            // UNIQUE
            if (column.getIsUnique() && !column.getIsPrimaryKey()) {
                colDef.append(" UNIQUE");
            }

            // COMMENT
            if (column.getComment() != null && !column.getComment().isEmpty()) {
                colDef.append(" COMMENT '").append(escapeSingleQuote(column.getComment())).append("'");
            } else if (column.getLogicalName() != null && !column.getLogicalName().isEmpty()) {
                colDef.append(" COMMENT '").append(escapeSingleQuote(column.getLogicalName())).append("'");
            }

            columnDefinitions.add(colDef.toString());

            // PRIMARY KEY 수집
            if (column.getIsPrimaryKey()) {
                primaryKeys.add("`" + column.getPhysicalName() + "`");
            }
        }

        sql.append(String.join(",\n", columnDefinitions));

        // PRIMARY KEY 제약조건
        if (!primaryKeys.isEmpty()) {
            sql.append(",\n  PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")");
        }

        sql.append("\n)");

        // 테이블 엔진 및 문자셋
        sql.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        // 테이블 코멘트
        if (table.getLogicalName() != null && !table.getLogicalName().isEmpty()) {
            sql.append(" COMMENT='").append(escapeSingleQuote(table.getLogicalName())).append("'");
        }

        sql.append(";\n");

        return sql.toString();
    }

    @Override
    public String generateForeignKey(ErdRelation relation, ErdTable fromTable, ErdColumn fromColumn,
                                     ErdTable toTable, ErdColumn toColumn) {
        StringBuilder sql = new StringBuilder();

        // 제약조건 이름 생성 규칙:
        // 1. 사용자 지정 이름이 있으면 사용
        // 2. 없으면 자동 생성: fk_{from_table}_{from_column}
        // 3. 길이 제한: MySQL은 64자
        String constraintName = relation.getConstraintName();
        if (constraintName == null || constraintName.trim().isEmpty()) {
            constraintName = generateForeignKeyName(fromTable.getPhysicalName(), fromColumn.getPhysicalName());
        }
        // MySQL identifier 최대 길이는 64자
        if (constraintName.length() > 64) {
            constraintName = constraintName.substring(0, 64);
        }

        sql.append("ALTER TABLE `").append(fromTable.getPhysicalName()).append("`\n");
        sql.append("  ADD CONSTRAINT `").append(constraintName).append("`\n");
        sql.append("  FOREIGN KEY (`").append(fromColumn.getPhysicalName()).append("`)\n");
        sql.append("  REFERENCES `").append(toTable.getPhysicalName()).append("`");
        sql.append(" (`").append(toColumn.getPhysicalName()).append("`)");

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
                return "INT";
            case "BIGINT":
                return "BIGINT";
            case "SMALLINT":
                return "SMALLINT";
            case "TINYINT":
                return "TINYINT";

            // 부동소수점
            case "FLOAT":
                return "FLOAT";
            case "DOUBLE":
                return "DOUBLE";
            case "DECIMAL":
            case "NUMERIC":
                if (dataDetail != null && dataDetail.length >= 2) {
                    return "DECIMAL(" + dataDetail[0] + ", " + dataDetail[1] + ")";
                } else if (dataDetail != null && dataDetail.length == 1) {
                    return "DECIMAL(" + dataDetail[0] + ")";
                }
                return "DECIMAL(10, 0)";

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
                return "TEXT";
            case "MEDIUMTEXT":
                return "MEDIUMTEXT";
            case "LONGTEXT":
                return "LONGTEXT";

            // 날짜/시간
            case "DATE":
                return "DATE";
            case "TIME":
                return "TIME";
            case "DATETIME":
                return "DATETIME";
            case "TIMESTAMP":
                return "TIMESTAMP";

            // Boolean
            case "BOOLEAN":
            case "BOOL":
                return "TINYINT(1)";

            // JSON
            case "JSON":
            case "JSONB":
                return "JSON";

            // Binary
            case "BLOB":
            case "BYTEA":
                return "BLOB";

            // UUID (MySQL에는 네이티브 UUID 타입이 없음)
            case "UUID":
                return "CHAR(36)";

            // 배열 타입 (MySQL은 배열을 지원하지 않으므로 JSON으로 변환)
            case "TEXT[]":
            case "VARCHAR[]":
            case "INT[]":
            case "INTEGER[]":
            case "BIGINT[]":
            case "SMALLINT[]":
            case "TINYINT[]":
                return "JSON"; // MySQL에서는 배열을 JSON으로 처리

            default:
                // 배열 타입 패턴 확인 ([]로 끝나는 경우)
                if (upperType.endsWith("[]")) {
                    return "JSON"; // MySQL은 배열을 JSON으로 변환
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
     * 작은따옴표 이스케이프
     */
    private String escapeSingleQuote(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''").replace("\\", "\\\\");
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
