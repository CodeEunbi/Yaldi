package com.yaldi.domain.datamodel.util;

import java.util.Map;

/**
 * 데이터베이스 타입을 Java/TypeScript 타입으로 매핑하는 유틸리티
 *
 * <p>PostgreSQL 데이터 타입을 Java 및 TypeScript 타입으로 변환합니다.</p>
 */
public class DataTypeMapper {

    /**
     * PostgreSQL 타입 → Java 타입 매핑
     */
    private static final Map<String, String> POSTGRES_TO_JAVA = Map.ofEntries(
            // 정수형
            Map.entry("SMALLINT", "Short"),
            Map.entry("INT2", "Short"),
            Map.entry("INTEGER", "Integer"),
            Map.entry("INT", "Integer"),
            Map.entry("INT4", "Integer"),
            Map.entry("BIGINT", "Long"),
            Map.entry("INT8", "Long"),

            // 실수형
            Map.entry("DECIMAL", "java.math.BigDecimal"),
            Map.entry("NUMERIC", "java.math.BigDecimal"),
            Map.entry("REAL", "Float"),
            Map.entry("FLOAT4", "Float"),
            Map.entry("DOUBLE PRECISION", "Double"),
            Map.entry("FLOAT8", "Double"),

            // 문자열
            Map.entry("CHAR", "String"),
            Map.entry("CHARACTER", "String"),
            Map.entry("VARCHAR", "String"),
            Map.entry("CHARACTER VARYING", "String"),
            Map.entry("TEXT", "String"),

            // 날짜/시간
            Map.entry("DATE", "java.time.LocalDate"),
            Map.entry("TIME", "java.time.LocalTime"),
            Map.entry("TIME WITHOUT TIME ZONE", "java.time.LocalTime"),
            Map.entry("TIME WITH TIME ZONE", "java.time.OffsetTime"),
            Map.entry("TIMESTAMP", "java.time.LocalDateTime"),
            Map.entry("TIMESTAMP WITHOUT TIME ZONE", "java.time.LocalDateTime"),
            Map.entry("TIMESTAMP WITH TIME ZONE", "java.time.OffsetDateTime"),
            Map.entry("TIMESTAMPTZ", "java.time.OffsetDateTime"),

            // 불린
            Map.entry("BOOLEAN", "Boolean"),
            Map.entry("BOOL", "Boolean"),

            // JSON
            Map.entry("JSON", "com.fasterxml.jackson.databind.JsonNode"),
            Map.entry("JSONB", "com.fasterxml.jackson.databind.JsonNode"),

            // UUID
            Map.entry("UUID", "java.util.UUID"),

            // 이진 데이터
            Map.entry("BYTEA", "byte[]")
    );

    /**
     * PostgreSQL 타입 → TypeScript 타입 매핑
     */
    private static final Map<String, String> POSTGRES_TO_TYPESCRIPT = Map.ofEntries(
            // 정수형
            Map.entry("SMALLINT", "number"),
            Map.entry("INT2", "number"),
            Map.entry("INTEGER", "number"),
            Map.entry("INT", "number"),
            Map.entry("INT4", "number"),
            Map.entry("BIGINT", "number"),
            Map.entry("INT8", "number"),

            // 실수형
            Map.entry("DECIMAL", "number"),
            Map.entry("NUMERIC", "number"),
            Map.entry("REAL", "number"),
            Map.entry("FLOAT4", "number"),
            Map.entry("DOUBLE PRECISION", "number"),
            Map.entry("FLOAT8", "number"),

            // 문자열
            Map.entry("CHAR", "string"),
            Map.entry("CHARACTER", "string"),
            Map.entry("VARCHAR", "string"),
            Map.entry("CHARACTER VARYING", "string"),
            Map.entry("TEXT", "string"),

            // 날짜/시간
            Map.entry("DATE", "Date"),
            Map.entry("TIME", "Date"),
            Map.entry("TIME WITHOUT TIME ZONE", "Date"),
            Map.entry("TIME WITH TIME ZONE", "Date"),
            Map.entry("TIMESTAMP", "Date"),
            Map.entry("TIMESTAMP WITHOUT TIME ZONE", "Date"),
            Map.entry("TIMESTAMP WITH TIME ZONE", "Date"),
            Map.entry("TIMESTAMPTZ", "Date"),

            // 불린
            Map.entry("BOOLEAN", "boolean"),
            Map.entry("BOOL", "boolean"),

            // JSON
            Map.entry("JSON", "any"),
            Map.entry("JSONB", "any"),

            // UUID
            Map.entry("UUID", "string"),

            // 이진 데이터
            Map.entry("BYTEA", "string")
    );

    /**
     * PostgreSQL 타입을 Java 타입으로 변환
     *
     * @param postgresType PostgreSQL 데이터 타입 (대소문자 무관)
     * @return Java 타입 문자열 (매핑 없으면 "Object")
     */
    public static String toJavaType(String postgresType) {
        if (postgresType == null || postgresType.isEmpty()) {
            return "Object";
        }

        String upperType = postgresType.toUpperCase().trim();
        return POSTGRES_TO_JAVA.getOrDefault(upperType, "Object");
    }

    /**
     * PostgreSQL 타입을 TypeScript 타입으로 변환
     *
     * @param postgresType PostgreSQL 데이터 타입 (대소문자 무관)
     * @return TypeScript 타입 문자열 (매핑 없으면 "any")
     */
    public static String toTypeScriptType(String postgresType) {
        if (postgresType == null || postgresType.isEmpty()) {
            return "any";
        }

        String upperType = postgresType.toUpperCase().trim();
        return POSTGRES_TO_TYPESCRIPT.getOrDefault(upperType, "any");
    }

    /**
     * Java 타입에서 import가 필요한지 확인
     *
     * @param javaType Java 타입 문자열
     * @return import 필요 여부
     */
    public static boolean needsImport(String javaType) {
        if (javaType == null) {
            return false;
        }

        // java.lang 패키지는 자동 import
        if (javaType.equals("String") || javaType.equals("Integer") ||
                javaType.equals("Long") || javaType.equals("Boolean") ||
                javaType.equals("Short") || javaType.equals("Float") ||
                javaType.equals("Double") || javaType.equals("Object")) {
            return false;
        }

        // 패키지명이 포함된 경우 import 필요
        return javaType.contains(".");
    }

    /**
     * Java 타입의 심플 이름 추출 (패키지명 제거)
     *
     * <p>예: "java.time.LocalDateTime" → "LocalDateTime"</p>
     *
     * @param fullJavaType 전체 Java 타입 이름
     * @return 심플 이름
     */
    public static String getSimpleName(String fullJavaType) {
        if (fullJavaType == null || fullJavaType.isEmpty()) {
            return fullJavaType;
        }

        int lastDotIndex = fullJavaType.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fullJavaType;
        }

        return fullJavaType.substring(lastDotIndex + 1);
    }
}
