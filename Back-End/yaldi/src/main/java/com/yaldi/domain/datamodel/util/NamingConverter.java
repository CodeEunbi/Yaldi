package com.yaldi.domain.datamodel.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 네이밍 변환 유틸리티
 *
 * <p>데이터베이스 네이밍(snake_case)을 Java/TypeScript 네이밍으로 변환합니다.</p>
 */
public class NamingConverter {

    /**
     * snake_case를 PascalCase로 변환
     *
     * <p>예시: user_profile → UserProfile</p>
     *
     * @param snakeCase snake_case 문자열
     * @return PascalCase 문자열
     */
    public static String toPascalCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isBlank()) {
            return "";
        }

        return Arrays.stream(snakeCase.split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> capitalize(part.toLowerCase()))
                .collect(Collectors.joining());
    }

    /**
     * snake_case를 camelCase로 변환
     *
     * <p>예시: user_id → userId</p>
     *
     * @param snakeCase snake_case 문자열
     * @return camelCase 문자열
     */
    public static String toCamelCase(String snakeCase) {
        if (snakeCase == null || snakeCase.isBlank()) {
            return "";
        }

        String[] parts = snakeCase.split("_");
        if (parts.length == 0) {
            return "";
        }

        // 첫 단어는 소문자, 나머지는 첫 글자만 대문자
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(capitalize(parts[i].toLowerCase()));
            }
        }

        return result.toString();
    }

    /**
     * 테이블 물리명으로 Entity 이름 생성
     *
     * <p>예시: users → UsersEntity</p>
     *
     * @param tablePhysicalName 테이블 물리명
     * @return Entity 이름
     */
    public static String toEntityName(String tablePhysicalName) {
        return toPascalCase(tablePhysicalName) + "Entity";
    }

    /**
     * 문자열의 첫 글자를 대문자로 변환
     *
     * @param str 입력 문자열
     * @return 첫 글자가 대문자인 문자열
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
