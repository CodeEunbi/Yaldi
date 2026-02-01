package com.yaldi.global.util;

/**
 * 문자열 처리 유틸리티
 */
public class StringUtils {

    /**
     * 문자열 정제 (trim + 개행문자 제거 + 연속 공백 제거)
     *
     * <p>다음과 같은 처리를 수행합니다:</p>
     * <ul>
     *   <li>앞뒤 공백 제거 (trim)</li>
     *   <li>개행문자(\n, \r) 제거</li>
     *   <li>탭 문자(\t) 제거</li>
     *   <li>연속된 공백을 하나의 공백으로 변환</li>
     * </ul>
     *
     * @param input 입력 문자열
     * @return 정제된 문자열, null이면 null 반환
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        return input
                .replaceAll("[\\r\\n\\t]", " ")   // 개행문자, 탭을 공백으로 변환
                .replaceAll("\\s+", " ")          // 연속 공백을 하나로
                .trim();                          // 앞뒤 공백 제거
    }

    /**
     * 문자열 정제 후 빈 문자열이면 null 반환
     *
     * @param input 입력 문자열
     * @return 정제된 문자열, 빈 문자열이면 null 반환
     */
    public static String sanitizeOrNull(String input) {
        String sanitized = sanitize(input);
        return (sanitized == null || sanitized.isEmpty()) ? null : sanitized;
    }

    /**
     * 문자열 정제 후 빈 문자열이면 기본값 반환
     *
     * @param input 입력 문자열
     * @param defaultValue 기본값
     * @return 정제된 문자열, 빈 문자열이면 기본값 반환
     */
    public static String sanitizeOrDefault(String input, String defaultValue) {
        String sanitized = sanitize(input);
        return (sanitized == null || sanitized.isEmpty()) ? defaultValue : sanitized;
    }

    /**
     * 닉네임 전용 정제 (공백 완전 제거)
     *
     * <p>닉네임은 공백을 허용하지 않으므로 모든 공백을 제거합니다.</p>
     *
     * @param nickname 닉네임
     * @return 정제된 닉네임
     */
    public static String sanitizeNickname(String nickname) {
        if (nickname == null) {
            return null;
        }

        return nickname
                .trim()                           // 앞뒤 공백 제거
                .replaceAll("[\\r\\n\\t\\s]", ""); // 모든 공백 문자 제거
    }
}
