package com.yaldi.domain.datamodel.codegen;

import com.yaldi.domain.datamodel.util.DataTypeMapper;
import com.yaldi.domain.datamodel.util.NamingConverter;
import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdTable;

import java.util.List;
import java.util.Map;

/**
 * TypeScript Interface 코드 생성기
 *
 * <p>ERD 정보로부터 TypeScript Interface를 생성합니다.</p>
 */
public class TypeScriptInterfaceCodeGenerator {

    private static final String INDENT = "  ";

    /**
     * TypeScript Interface 생성 (Entity용)
     *
     * @param interfaceName Interface 이름 (예: UsersEntity)
     * @param table ERD 테이블 정보
     * @param columns 컬럼 목록
     * @return 생성된 TypeScript Interface 코드
     */
    public static String generateForEntity(String interfaceName, ErdTable table, List<ErdColumn> columns) {
        StringBuilder code = new StringBuilder();

        // JSDoc
        code.append("/**\n");
        code.append(" * ").append(interfaceName).append(" - ").append(table.getLogicalName()).append("\n");
        code.append(" *\n");
        code.append(" * 테이블: ").append(table.getPhysicalName()).append("\n");
        code.append(" */\n");

        // Interface Declaration
        code.append("export interface ").append(interfaceName).append(" {\n");

        // Fields
        for (int i = 0; i < columns.size(); i++) {
            ErdColumn column = columns.get(i);
            String fieldName = NamingConverter.toCamelCase(column.getPhysicalName());

            code.append(generateField(column, fieldName));

            // 마지막 필드가 아니면 빈 줄 추가
            if (i < columns.size() - 1) {
                code.append("\n");
            }
        }

        // Interface End
        code.append("}\n");

        return code.toString();
    }

    /**
     * TypeScript Interface 생성 (DTO용)
     *
     * @param interfaceName Interface 이름 (예: UserCreateRequest, UserResponse)
     * @param columns 컬럼 목록
     * @param columnAliases 컬럼 별칭 맵 (columnKey → alias)
     * @return 생성된 TypeScript Interface 코드
     */
    public static String generateForDto(String interfaceName, List<ErdColumn> columns, Map<Long, String> columnAliases) {
        StringBuilder code = new StringBuilder();

        // JSDoc
        code.append("/**\n");
        code.append(" * ").append(interfaceName).append("\n");
        code.append(" */\n");

        // Interface Declaration
        code.append("export interface ").append(interfaceName).append(" {\n");

        // Fields
        for (int i = 0; i < columns.size(); i++) {
            ErdColumn column = columns.get(i);
            String fieldName = columnAliases.getOrDefault(column.getColumnKey(),
                    NamingConverter.toCamelCase(column.getPhysicalName()));

            code.append(generateField(column, fieldName));

            // 마지막 필드가 아니면 빈 줄 추가
            if (i < columns.size() - 1) {
                code.append("\n");
            }
        }

        // Interface End
        code.append("}\n");

        return code.toString();
    }

    /**
     * 필드 코드 생성
     */
    private static String generateField(ErdColumn column, String fieldName) {
        StringBuilder field = new StringBuilder();

        String tsType = DataTypeMapper.toTypeScriptType(column.getDataType());

        // JSDoc
        field.append(INDENT).append("/**\n");
        field.append(INDENT).append(" * ").append(column.getLogicalName());
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            field.append(" - ").append(column.getComment());
        }
        field.append("\n");
        field.append(INDENT).append(" */\n");

        // Field Declaration
        field.append(INDENT).append(fieldName);

        // Optional field (nullable)
        if (column.getIsNullable()) {
            field.append("?");
        }

        field.append(": ").append(tsType).append(";\n");

        return field.toString();
    }
}
