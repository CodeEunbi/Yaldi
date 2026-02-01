package com.yaldi.domain.datamodel.codegen;

import com.yaldi.domain.datamodel.entity.DataModelType;
import com.yaldi.domain.datamodel.util.DataTypeMapper;
import com.yaldi.domain.datamodel.util.NamingConverter;
import com.yaldi.domain.erd.entity.ErdColumn;

import java.util.*;

/**
 * Java DTO 코드 생성기
 *
 * <p>ERD 컬럼 정보로부터 Java Record 기반 DTO를 생성합니다.</p>
 */
public class JavaDtoCodeGenerator {

    private static final String INDENT = "    ";

    /**
     * DTO 코드 생성
     *
     * @param dtoName DTO 클래스명 (예: UserCreateRequest, UserResponse)
     * @param type DTO 타입 (DTO_REQUEST 또는 DTO_RESPONSE)
     * @param columns 컬럼 목록
     * @param columnAliases 컬럼 별칭 맵 (columnKey → alias)
     * @return 생성된 Java DTO 코드
     */
    public static String generate(String dtoName, DataModelType type, List<ErdColumn> columns, Map<Long, String> columnAliases) {
        StringBuilder code = new StringBuilder();

        // Package
        String packagePath = type == DataModelType.DTO_REQUEST ? "com.example.dto.request" : "com.example.dto.response";
        code.append("package ").append(packagePath).append(";\n\n");

        // Imports
        Set<String> imports = collectImports(type, columns);
        for (String importStatement : imports) {
            code.append("import ").append(importStatement).append(";\n");
        }
        code.append("\n");

        // Class JavaDoc
        code.append("/**\n");
        code.append(" * ").append(dtoName);
        if (type == DataModelType.DTO_REQUEST) {
            code.append(" - 요청 DTO");
        } else {
            code.append(" - 응답 DTO");
        }
        code.append("\n");
        code.append(" */\n");

        // Class Annotation
        String description = type == DataModelType.DTO_REQUEST ? dtoName + " 요청" : dtoName + " 응답";
        code.append("@Schema(description = \"").append(description).append("\")\n");

        // Record Declaration
        code.append("public record ").append(dtoName).append("(\n");

        // Fields
        for (int i = 0; i < columns.size(); i++) {
            ErdColumn column = columns.get(i);
            String alias = columnAliases.getOrDefault(column.getColumnKey(),
                    NamingConverter.toCamelCase(column.getPhysicalName()));

            code.append(generateField(column, alias, type));

            // 마지막 필드가 아니면 쉼표 추가
            if (i < columns.size() - 1) {
                code.append(",\n\n");
            } else {
                code.append("\n");
            }
        }

        // Record End
        code.append(") {\n");
        code.append("}\n");

        return code.toString();
    }

    /**
     * 필드 코드 생성
     */
    private static String generateField(ErdColumn column, String alias, DataModelType type) {
        StringBuilder field = new StringBuilder();

        String javaType = DataTypeMapper.toJavaType(column.getDataType());
        String simpleType = DataTypeMapper.getSimpleName(javaType);

        // Validation annotations (Request DTO only)
        if (type == DataModelType.DTO_REQUEST) {
            if (!column.getIsNullable()) {
                // 필수 필드 validation
                if (isStringType(column.getDataType())) {
                    field.append(INDENT).append("@NotBlank(message = \"")
                            .append(column.getLogicalName()).append("은(는) 필수입니다\")\n");

                    // 문자열 길이 제한
                    if (column.getDataDetail() != null && column.getDataDetail().length > 0) {
                        try {
                            int maxLength = Integer.parseInt(column.getDataDetail()[0]);
                            field.append(INDENT).append("@Size(max = ").append(maxLength)
                                    .append(", message = \"").append(column.getLogicalName())
                                    .append("은(는) ").append(maxLength).append("자 이하이어야 합니다\")\n");
                        } catch (NumberFormatException ignored) {
                            // 파싱 실패 시 무시
                        }
                    }
                } else {
                    field.append(INDENT).append("@NotNull(message = \"")
                            .append(column.getLogicalName()).append("은(는) 필수입니다\")\n");
                }
            }
        }

        // Swagger annotation
        field.append(INDENT).append("@Schema(");
        List<String> schemaAttrs = new ArrayList<>();

        schemaAttrs.add("description = \"" + column.getLogicalName() + "\"");

        // example 값 생성
        String example = generateExampleValue(column);
        if (example != null) {
            schemaAttrs.add("example = \"" + example + "\"");
        }

        // requiredMode (Request DTO && not nullable)
        if (type == DataModelType.DTO_REQUEST && !column.getIsNullable()) {
            schemaAttrs.add("requiredMode = Schema.RequiredMode.REQUIRED");
        }

        field.append(String.join(", ", schemaAttrs));
        field.append(")\n");

        // Field Declaration
        field.append(INDENT).append(simpleType).append(" ").append(alias);

        return field.toString();
    }

    /**
     * Import 문 수집
     */
    private static Set<String> collectImports(DataModelType type, List<ErdColumn> columns) {
        Set<String> imports = new TreeSet<>();

        // Swagger import
        imports.add("io.swagger.v3.oas.annotations.media.Schema");

        // Validation imports (Request DTO only)
        if (type == DataModelType.DTO_REQUEST) {
            boolean hasNotBlank = false;
            boolean hasNotNull = false;
            boolean hasSize = false;

            for (ErdColumn column : columns) {
                if (!column.getIsNullable()) {
                    if (isStringType(column.getDataType())) {
                        hasNotBlank = true;
                        if (column.getDataDetail() != null && column.getDataDetail().length > 0) {
                            hasSize = true;
                        }
                    } else {
                        hasNotNull = true;
                    }
                }
            }

            if (hasNotBlank) imports.add("jakarta.validation.constraints.NotBlank");
            if (hasNotNull) imports.add("jakarta.validation.constraints.NotNull");
            if (hasSize) imports.add("jakarta.validation.constraints.Size");
        }

        // 데이터 타입별 import
        for (ErdColumn column : columns) {
            String javaType = DataTypeMapper.toJavaType(column.getDataType());
            if (DataTypeMapper.needsImport(javaType)) {
                imports.add(javaType);
            }
        }

        return imports;
    }

    /**
     * 문자열 타입 여부 확인
     */
    private static boolean isStringType(String dataType) {
        if (dataType == null) {
            return false;
        }
        String upper = dataType.toUpperCase();
        return upper.contains("CHAR") || upper.equals("TEXT");
    }

    /**
     * Example 값 생성
     */
    private static String generateExampleValue(ErdColumn column) {
        String dataType = column.getDataType().toUpperCase();

        if (isStringType(dataType)) {
            return column.getLogicalName() + " 예시";
        } else if (dataType.contains("INT") || dataType.contains("DECIMAL") ||
                   dataType.contains("NUMERIC") || dataType.contains("REAL") ||
                   dataType.contains("DOUBLE")) {
            return "1";
        } else if (dataType.equals("BOOLEAN") || dataType.equals("BOOL")) {
            return "true";
        } else if (dataType.contains("TIMESTAMP") || dataType.contains("DATE")) {
            return "2024-01-01T00:00:00";
        }

        return null;
    }
}
