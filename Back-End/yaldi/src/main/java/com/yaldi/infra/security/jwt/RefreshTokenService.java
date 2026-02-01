package com.yaldi.infra.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token을 Redis에 저장하고 관리하는 서비스
 *
 * <h3>설계 목적</h3>
 * <p>JWT의 핵심 장점인 <strong>병목현상 완화</strong>를 달성하기 위해,
 * Access Token은 서버에 저장하지 않고 <strong>Refresh Token만 Redis에 저장</strong>합니다.</p>
 *
 * <h3>병목현상 완화 원리</h3>
 * <ul>
 *   <li><strong>Access Token:</strong> 서버에 저장하지 않음. 매 요청마다 서명만 검증하여 DB 조회 없이 인증</li>
 *   <li><strong>Refresh Token:</strong> Redis에 저장하여 로그아웃 및 강제 만료 가능</li>
 * </ul>
 *
 * <p>동시접속자 1천만명 기준, Access Token을 서버에 저장하지 않음으로써
 * 약 2천만 개의 DB 연결을 절감할 수 있습니다.</p>
 *
 * <h3>Redis 사용 이유</h3>
 * <ul>
 *   <li>In-Memory 저장으로 빠른 읽기/쓰기 성능</li>
 *   <li>TTL(Time To Live) 자동 만료 지원</li>
 *   <li>RDB 대비 낮은 부하</li>
 * </ul>
 *
 * <h3>보안 고려사항</h3>
 * <ul>
 *   <li>Refresh Token은 한 사용자당 하나만 유지 (중복 로그인 방지 가능)</li>
 *   <li>로그아웃 시 Redis에서 토큰 삭제로 즉시 무효화</li>
 *   <li>Refresh Token 탈취 시 Redis 삭제로 피해 최소화</li>
 * </ul>
 *
 * @author Yaldi Team
 * @version 1.0
 * @see JwtProperties
 * @see RedisTemplate
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;

    /** Redis Key 접두사: refresh_token:{userKey} 형식으로 저장 */
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token을 Redis에 저장합니다.
     *
     * <p>사용자별로 하나의 Refresh Token만 유지하므로,
     * 새로운 토큰을 저장하면 기존 토큰은 자동으로 덮어씌워집니다.
     * 이를 통해 중복 로그인을 제어할 수 있습니다.</p>
     *
     * <h4>Redis 저장 형식:</h4>
     * <pre>
     * Key: refresh_token:{userKey}
     * Value: {refreshToken}
     * TTL: jwt.refresh-token-expiration (기본 7일)
     * </pre>
     *
     * <p>TTL이 지나면 Redis에서 자동으로 삭제되므로 별도의 정리 작업이 필요 없습니다.</p>
     *
     * @param userKey 사용자 고유 식별자
     * @param refreshToken 저장할 Refresh Token 문자열
     */
    public void saveRefreshToken(Integer userKey, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userKey;
        long expiration = jwtProperties.getRefreshTokenExpiration();

        // Redis에 토큰 저장 (TTL 자동 설정)
        redisTemplate.opsForValue().set(key, refreshToken, expiration, TimeUnit.MILLISECONDS);
        log.debug("Saved refresh token for user: {}", userKey);
    }

    /**
     * Redis에서 사용자의 Refresh Token을 조회합니다.
     *
     * <p>토큰 갱신 API에서 클라이언트가 전송한 Refresh Token과
     * Redis에 저장된 토큰을 비교하여 유효성을 검증하는 데 사용됩니다.</p>
     *
     * @param userKey 사용자 고유 식별자
     * @return Redis에 저장된 Refresh Token. 존재하지 않거나 만료된 경우 null
     */
    public String getRefreshToken(Integer userKey) {
        String key = REFRESH_TOKEN_PREFIX + userKey;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 클라이언트가 전송한 Refresh Token이 유효한지 검증합니다.
     *
     * <p>Redis에 저장된 토큰과 클라이언트가 전송한 토큰을 비교하여
     * 일치 여부를 확인합니다. 이를 통해 다음을 방지합니다:</p>
     * <ul>
     *   <li>만료된 토큰 재사용</li>
     *   <li>로그아웃된 사용자의 토큰 사용</li>
     *   <li>탈취된 토큰 사용 (강제 만료 후)</li>
     * </ul>
     *
     * @param userKey 사용자 고유 식별자
     * @param refreshToken 검증할 Refresh Token 문자열
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateRefreshToken(Integer userKey, String refreshToken) {
        String savedToken = getRefreshToken(userKey);
        return savedToken != null && savedToken.equals(refreshToken);
    }

    /**
     * Redis에서 사용자의 Refresh Token을 삭제합니다.
     *
     * <p>다음 상황에서 호출됩니다:</p>
     * <ul>
     *   <li><strong>로그아웃:</strong> 사용자가 명시적으로 로그아웃할 때</li>
     *   <li><strong>강제 만료:</strong> 보안 이슈로 토큰을 무효화해야 할 때</li>
     *   <li><strong>탈퇴:</strong> 사용자 계정이 삭제될 때</li>
     * </ul>
     *
     * <p>삭제 후에는 해당 Refresh Token으로 Access Token을 갱신할 수 없습니다.</p>
     *
     * @param userKey 사용자 고유 식별자
     */
    public void deleteRefreshToken(Integer userKey) {
        String key = REFRESH_TOKEN_PREFIX + userKey;
        redisTemplate.delete(key);
        log.debug("Deleted refresh token for user: {}", userKey);
    }

    /**
     * 사용자의 Refresh Token이 Redis에 존재하는지 확인합니다.
     *
     * <p>로그인 상태 확인이나 중복 로그인 방지 로직에서 사용할 수 있습니다.</p>
     *
     * @param userKey 사용자 고유 식별자
     * @return Refresh Token이 존재하면 true, 그렇지 않으면 false
     */
    public boolean hasRefreshToken(Integer userKey) {
        String key = REFRESH_TOKEN_PREFIX + userKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Refresh Token의 남은 유효 시간을 조회합니다 (초 단위).
     *
     * <p>웹 환경에서 pre-refresh 메커니즘을 구현할 때 유용합니다.
     * 만료 시간이 임박한 경우 자동 갱신 여부를 결정하는 데 사용할 수 있습니다.</p>
     *
     * <p><strong>예시:</strong> TTL이 1일 미만이면 사용자 활동 시 자동 갱신</p>
     *
     * @param userKey 사용자 고유 식별자
     * @return 남은 유효 시간 (초 단위). 키가 존재하지 않으면 -2, 만료 시간이 없으면 -1
     */
    public Long getRefreshTokenTTL(Integer userKey) {
        String key = REFRESH_TOKEN_PREFIX + userKey;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
