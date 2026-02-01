package com.yaldi.infra.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 연결 및 RedisTemplate 설정
 *
 * <h3>Redis 사용 목적</h3>
 * <p>JWT Refresh Token을 저장하여 다음을 달성합니다:</p>
 * <ul>
 *   <li><strong>병목현상 완화:</strong> RDB 대신 In-Memory 저장소 사용으로 빠른 조회</li>
 *   <li><strong>TTL 자동 관리:</strong> 만료된 토큰 자동 삭제</li>
 *   <li><strong>로그아웃 지원:</strong> 토큰 즉시 무효화 가능</li>
 * </ul>
 *
 * <h3>Lettuce vs Jedis</h3>
 * <p>Lettuce를 사용하는 이유:</p>
 * <ul>
 *   <li>비동기/논블로킹 I/O 지원 (Netty 기반)</li>
 *   <li>Thread-safe한 연결 관리</li>
 *   <li>Spring Boot 2.x 이상 기본 클라이언트</li>
 *   <li>Connection Pool 자동 관리</li>
 * </ul>
 *
 * <h3>설정 구조</h3>
 * <pre>
 * application.yml:
 *   spring.data.redis.host: localhost
 *   spring.data.redis.port: 6379
 *   spring.data.redis.password: redis
 * </pre>
 *
 * <h3>참고</h3>
 * <ul>
 *   <li><strong>ObjectMapper Bean:</strong> 전역 ObjectMapper는 WebMvcConfig로 이동됨 (책임 분리)</li>
 *   <li>jsonRedisTemplate은 WebMvcConfig의 ObjectMapper Bean을 주입받아 사용</li>
 * </ul>
 *
 * @author Yaldi Team
 * @version 1.0
 * @see LettuceConnectionFactory
 * @see RedisTemplate
 * @see com.yaldi.global.config.WebMvcConfig
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /** Redis 서버 호스트 주소 (기본값: localhost) */
    @Value("${spring.data.redis.host}")
    private String host;

    /** Redis 서버 포트 (기본값: 6379) */
    @Value("${spring.data.redis.port}")
    private int port;

    /** Redis 인증 비밀번호 */
    @Value("${spring.data.redis.password}")
    private String password;

    /**
     * Redis 연결 팩토리를 생성합니다.
     *
     * <p>Lettuce 기반의 연결 팩토리를 생성하며, 다음 설정이 적용됩니다:</p>
     * <ul>
     *   <li><strong>Standalone Mode:</strong> 단일 Redis 서버 연결</li>
     *   <li><strong>Connection Pool:</strong> application.yml의 lettuce.pool 설정 적용</li>
     *   <li><strong>인증:</strong> 비밀번호 기반 인증</li>
     * </ul>
     *
     * <p><strong>Production 환경 고려사항:</strong></p>
     * <ul>
     *   <li>Redis Sentinel 또는 Cluster 사용 권장</li>
     *   <li>Connection Pool 크기 조정 필요</li>
     *   <li>네트워크 타임아웃 설정</li>
     * </ul>
     *
     * @return Lettuce 기반 RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Standalone 모드 설정
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(password);

        // Lettuce Connection Factory 생성
        // application.yml의 lettuce.pool 설정이 자동으로 적용됨
        return new LettuceConnectionFactory(config);
    }

    /**
     * RedisTemplate을 생성하고 설정합니다.
     *
     * <p>RedisTemplate은 Redis 데이터 조작을 위한 고수준 추상화 계층입니다.
     * String 타입의 Key-Value 저장소로 설정됩니다.</p>
     *
     * <h4>직렬화 설정</h4>
     * <p>StringRedisSerializer를 사용하여 다음을 보장합니다:</p>
     * <ul>
     *   <li><strong>가독성:</strong> Redis CLI에서 직접 읽을 수 있는 문자열 형태</li>
     *   <li><strong>호환성:</strong> 다른 애플리케이션과의 데이터 공유 가능</li>
     *   <li><strong>크기:</strong> JDK 직렬화 대비 작은 데이터 크기</li>
     * </ul>
     *
     * <h4>사용 예시</h4>
     * <pre>
     * redisTemplate.opsForValue().set("key", "value");
     * String value = redisTemplate.opsForValue().get("key");
     * </pre>
     *
     * @return 설정된 RedisTemplate<String, String> 인스턴스
     * @see StringRedisSerializer
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        // StringRedisSerializer 사용으로 사람이 읽을 수 있는 형태로 저장
        // (JDK 직렬화를 사용하면 바이너리 형태로 저장되어 가독성 떨어짐)
        StringRedisSerializer serializer = new StringRedisSerializer();

        // Key 직렬화: "refresh_token:123" 형태
        template.setKeySerializer(serializer);

        // Value 직렬화: JWT 토큰 문자열
        template.setValueSerializer(serializer);

        // Hash 자료구조 사용 시 Key/Value 직렬화
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        return template;
    }

    /**
     * JSON 직렬화를 위한 RedisTemplate 설정
     *
     * <p>복잡한 객체를 Redis에 저장할 때 사용합니다 (WebSocket 메시지, 캐시 등).</p>
     *
     * <h4>일반 redisTemplate과의 차이점</h4>
     * <ul>
     *   <li><strong>일반 redisTemplate:</strong> String ↔ String (JWT 토큰 등 단순 문자열)</li>
     *   <li><strong>jsonRedisTemplate:</strong> String ↔ Object (복잡한 객체를 JSON으로 직렬화)</li>
     * </ul>
     *
     * @param factory Redis 연결 팩토리
     * @param objectMapper 전역 ObjectMapper Bean (WebMvcConfig에서 주입)
     * @return JSON 직렬화가 설정된 RedisTemplate<String, Object>
     */
    @Bean(name = "jsonRedisTemplate")
    public RedisTemplate<String, Object> jsonRedisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 전역 ObjectMapper Bean을 사용하여 일관된 직렬화 설정 적용
        // (null 값 포함, ISO-8601 날짜 형식, UTC 타임존 등)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // Key는 문자열, Value는 JSON으로 직렬화
        template.setKeySerializer(keySerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    /**
     * Redis 기반 Cache Manager 설정
     *
     * <p>Spring의 @Cacheable, @CacheEvict, @CachePut 어노테이션을 사용하여
     * 메서드 결과를 Redis에 캐싱할 수 있도록 합니다.</p>
     *
     * <h3>사용 목적</h3>
     * <p>JWT 기반 인증 환경에서 권한 정보를 캐싱하여 DB 조회를 최소화합니다:</p>
     * <ul>
     *   <li><strong>문제:</strong> JWT에 권한 정보를 포함하면 권한 변경 시 즉시 반영 불가</li>
     *   <li><strong>해결:</strong> JWT에는 최소 정보만 포함, 권한은 Redis 캐싱으로 조회</li>
     *   <li><strong>성능:</strong> 매 요청마다 DB 조회 대신 Redis 조회 (평균 0.5ms)</li>
     * </ul>
     *
     * <h3>캐시 설정</h3>
     * <ul>
     *   <li><strong>TTL:</strong> 5분 (권한 변경 시 최대 5분 후 반영)</li>
     *   <li><strong>Key Prefix:</strong> cache 이름 자동 추가 (예: project:member:role::123:456)</li>
     *   <li><strong>Null 값 캐싱:</strong> 비활성화 (null 결과는 캐싱하지 않음)</li>
     * </ul>
     *
     * <h3>사용 예시</h3>
     * <pre>
     * {@code @Cacheable(value = "project:member:role", key = "#projectKey + ':' + #userKey")}
     * public ProjectMemberRole getMemberRole(Long projectKey, Integer userKey) {
     *     return repository.findRole(projectKey, userKey);
     * }
     * </pre>
     *
     * @param factory Redis 연결 팩토리
     * @param objectMapper JSON 직렬화용 ObjectMapper
     * @return RedisCacheManager 인스턴스
     */
    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory factory,
            ObjectMapper objectMapper) {

        // JSON 직렬화 설정
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // TTL 5분 (권한 변경 시 최대 5분 후 반영)
                .entryTtl(Duration.ofMinutes(5))
                // Key 직렬화: String
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                // Value 직렬화: JSON
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                // null 값은 캐싱하지 않음
                .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .build();
    }
}

