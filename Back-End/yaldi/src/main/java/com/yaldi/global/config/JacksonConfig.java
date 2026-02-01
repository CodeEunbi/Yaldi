package com.yaldi.global.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Jackson 설정
 *
 * <p>JSON 직렬화/역직렬화 시 문자열 자동 정제 설정 및 전역 ObjectMapper 구성</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li><strong>문자열 자동 정제:</strong> 모든 String 필드의 공백, 개행문자 자동 trim</li>
 *   <li><strong>null 값 포함:</strong> null 필드도 JSON 응답에 포함 (deletedAt 등)</li>
 *   <li><strong>날짜 형식:</strong> ISO-8601 문자열 형태로 직렬화</li>
 *   <li><strong>타임존:</strong> UTC 기준</li>
 *   <li><strong>유연한 역직렬화:</strong> 알 수 없는 필드 무시</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    /**
     * 문자열 자동 trim Deserializer
     *
     * <p>모든 문자열 입력에 대해 다음 처리를 수행합니다:</p>
     * <ul>
     *   <li>개행문자(\n, \r), 탭(\t)을 공백으로 변환</li>
     *   <li>연속된 공백을 하나의 공백으로 압축</li>
     *   <li>앞뒤 공백 제거 (trim)</li>
     *   <li>빈 문자열은 null로 변환</li>
     * </ul>
     */
    public static class TrimmingStringDeserializer extends StdScalarDeserializer<String> {

        public TrimmingStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null) {
                return null;
            }

            // trim + 개행문자/탭 처리
            String trimmed = value
//                    .replaceAll("[\\r\\n\\t]", " ")  // 개행문자, 탭을 공백으로
//                    .replaceAll("\\s+", " ")         // 연속 공백을 하나로
                    .trim();                         // 앞뒤 공백 제거

            // 빈 문자열은 null로 변환
            return trimmed.isEmpty() ? null : trimmed;
        }
    }

    /**
     * Jackson ObjectMapper 전역 설정
     *
     * <p>Spring MVC 전역 범위에서 사용되며, HTTP API 응답/요청 전체에 영향을 미칩니다
     * (@RestController, @RequestBody, @ResponseBody 등)</p>
     *
     * @return 설정된 ObjectMapper 인스턴스
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Java 8 날짜/시간 타입 지원 (LocalDateTime, OffsetDateTime 등)
        objectMapper.registerModule(new JavaTimeModule());

        // 날짜를 타임스탬프 숫자가 아닌 ISO-8601 문자열로 직렬화
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // UTC 타임존 설정 (데이터베이스 TIMESTAMPTZ와 일관성 유지)
        objectMapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

        // null 값도 JSON에 포함 (deletedAt, description 등 null 필드 표시)
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 역직렬화 시 알 수 없는 속성이 있어도 에러 발생 안함
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 역직렬화 시 생성자 파라미터가 없어도 에러 발생 안함
        objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);

        // 빈 문자열을 null로 처리
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        // String 타입에 대해 자동 trim 적용
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new TrimmingStringDeserializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }
}
