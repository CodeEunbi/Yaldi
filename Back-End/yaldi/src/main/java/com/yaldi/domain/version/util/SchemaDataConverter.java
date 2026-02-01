package com.yaldi.domain.version.util;

import com.yaldi.domain.erd.entity.ReferentialActionType;
import com.yaldi.domain.erd.entity.RelationType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Schema JSON 데이터를 Entity로 변환하는 유틸리티 클래스
 * - null 안전 변환
 * - 타입 변환 에러 방지
 */
public class SchemaDataConverter {

    private SchemaDataConverter() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * Object를 안전하게 List<Map<String, Object>>로 변환
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> safeList(Object obj) {
        if (obj instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return Collections.emptyList();
    }

    /**
     * Object를 Long으로 변환 (null인 경우 -1L 반환)
     */
    public static Long toLong(Object obj) {
        if (obj instanceof Integer i) {
            return i.longValue();
        }
        if (obj instanceof Long l) {
            return l;
        }
        if (obj == null) {
            return -1L;
        }
        return Long.parseLong(obj.toString());
    }

    /**
     * Object를 Long으로 변환 (null인 경우 null 반환)
     */
    public static Long toLongOrNull(Object obj) {
        if (obj == null) {
            return null;
        }
        return toLong(obj);
    }

    /**
     * Object를 Integer로 변환 (기본값 지원)
     */
    public static Integer toInteger(Object obj, int defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Integer i) {
            return i;
        }
        return Integer.parseInt(obj.toString());
    }

    /**
     * Object를 Boolean으로 변환 (기본값 지원)
     */
    public static Boolean toBoolean(Object obj, boolean defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(obj.toString());
    }

    /**
     * Object를 BigDecimal로 변환 (기본값 지원)
     */
    public static BigDecimal toBigDecimal(Object obj, BigDecimal defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Object를 String으로 안전하게 변환 (기본값 지원)
     */
    public static String toStringSafe(Object obj, String defaultValue) {
        return obj == null ? defaultValue : obj.toString();
    }

    /**
     * Object를 String 배열로 변환
     * List인 경우 각 요소를 String으로 변환
     */
    public static String[] toArray(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof List<?> list) {
            return list.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
        }
        return null;
    }

    /**
     * Object를 RelationType enum으로 변환 (기본값: OPTIONAL_ONE_TO_MANY)
     */
    public static RelationType toRelationType(Object obj) {
        if (obj == null) {
            return RelationType.OPTIONAL_ONE_TO_MANY;
        }
        try {
            return RelationType.valueOf(obj.toString());
        } catch (Exception e) {
            return RelationType.OPTIONAL_ONE_TO_MANY;
        }
    }

    /**
     * Object를 ReferentialActionType enum으로 변환 (기본값: NO_ACTION)
     */
    public static ReferentialActionType toReferentialActionType(Object obj) {
        if (obj == null) {
            return ReferentialActionType.NO_ACTION;
        }
        try {
            return ReferentialActionType.valueOf(obj.toString());
        } catch (Exception e) {
            return ReferentialActionType.NO_ACTION;
        }
    }
}
