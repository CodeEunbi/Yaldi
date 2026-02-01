package com.yaldi.domain.datamodel.codegen;

import com.yaldi.domain.datamodel.util.DataTypeMapper;
import com.yaldi.domain.datamodel.util.NamingConverter;
import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdTable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Java Entity 코드 생성기
 *
 * <p>ERD 테이블 정보로부터 JPA Entity 클래스를 생성합니다.</p>
 */
public class JavaEntityCodeGenerator {

    private static final String INDENT = "    ";
    private static final String DOUBLE_INDENT = INDENT + INDENT;

    /**
     * Entity 코드 생성
     *
     * @param entityName Entity 클래스명 (예: UsersEntity)
     * @param table ERD 테이블 정보
     * @param columns 컬럼 목록
     * @return 생성된 Java Entity 코드
     */
    public static String generate(String entityName, ErdTable table, List<ErdColumn> columns) {
        StringBuilder code = new StringBuilder();

        // Package (임시로 com.example.entity 사용, 실제로는 프로젝트 설정에서 가져와야 함)
        code.append("package com.example.entity;\n\n");

        // Imports
        Set<String> imports = collectImports(columns);
        for (String importStatement : imports) {
            code.append("import ").append(importStatement).append(";\n");
        }
        code.append("\n");

        // Class JavaDoc
        code.append("/**\n");
        code.append(" * ").append(entityName).append(" - ").append(table.getLogicalName()).append("\n");
        code.append(" *\n");
        code.append(" * <p>테이블: ").append(table.getPhysicalName()).append("</p>\n");
        code.append(" */\n");

        // Class Annotations
        code.append("@Entity\n");
        code.append("@Table(name = \"").append(table.getPhysicalName()).append("\")\n");
        code.append("@Getter\n");
        code.append("@Setter\n");
        code.append("@NoArgsConstructor\n");
        code.append("@AllArgsConstructor\n");
        code.append("@Builder\n");

        // Class Declaration
        code.append("public class ").append(entityName).append(" {\n\n");

        // Fields
        for (int i = 0; i < columns.size(); i++) {
            ErdColumn column = columns.get(i);
            code.append(generateField(column));

            // 마지막 필드가 아니면 빈 줄 추가
            if (i < columns.size() - 1) {
                code.append("\n");
            }
        }

        // Class End
        code.append("}\n");

        return code.toString();
    }

    /**
     * 필드 코드 생성
     */
    private static String generateField(ErdColumn column) {
        StringBuilder field = new StringBuilder();

        String javaType = DataTypeMapper.toJavaType(column.getDataType());
        String simpleType = DataTypeMapper.getSimpleName(javaType);
        String fieldName = NamingConverter.toCamelCase(column.getPhysicalName());

        // JavaDoc
        field.append(INDENT).append("/**\n");
        field.append(INDENT).append(" * ").append(column.getLogicalName());
        if (column.getComment() != null && !column.getComment().isEmpty()) {
            field.append(" - ").append(column.getComment());
        }
        field.append("\n");
        field.append(INDENT).append(" */\n");

        // Annotations
        if (column.getIsPrimaryKey()) {
            field.append(INDENT).append("@Id\n");
            if (column.getIsIncremental()) {
                field.append(INDENT).append("@GeneratedValue(strategy = GenerationType.IDENTITY)\n");
            }
        }

        // @Column annotation
        field.append(INDENT).append("@Column(");
        List<String> columnAttrs = new ArrayList<>();
        columnAttrs.add("name = \"" + column.getPhysicalName() + "\"");

        // length 속성 (VARCHAR, CHAR)
        if (isStringType(column.getDataType()) && column.getDataDetail() != null && column.getDataDetail().length > 0) {
            try {
                int length = Integer.parseInt(column.getDataDetail()[0]);
                columnAttrs.add("length = " + length);
            } catch (NumberFormatException ignored) {
                // 파싱 실패 시 무시
            }
        }

        // nullable 속성
        if (!column.getIsNullable()) {
            columnAttrs.add("nullable = false");
        }

        // unique 속성
        if (column.getIsUnique()) {
            columnAttrs.add("unique = true");
        }

        field.append(String.join(", ", columnAttrs));
        field.append(")\n");

        // Field Declaration
        field.append(INDENT).append("private ").append(simpleType).append(" ").append(fieldName).append(";\n");

        return field.toString();
    }

    /**
     * Import 문 수집
     */
    private static Set<String> collectImports(List<ErdColumn> columns) {
        Set<String> imports = new TreeSet<>();  // 알파벳 순 정렬

        // JPA imports
        imports.add("jakarta.persistence.*");

        // Lombok imports
        imports.add("lombok.*");

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
}
