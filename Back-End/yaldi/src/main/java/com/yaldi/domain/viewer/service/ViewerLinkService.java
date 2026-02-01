package com.yaldi.domain.viewer.service;

import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.domain.viewer.dto.ViewerLinkInfo;
import com.yaldi.domain.viewer.dto.response.ViewerLinkResponse;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Forward Mapping (projectKey → linkId 찾기)
 * 용도: 프로젝트 멤버가 링크 생성 시, 기존 링크 있는지 확인
 * Reverse Mapping (linkId → projectKey 찾기)
 * 용도: 뷰어가 링크 클릭 시, 어떤 프로젝트인지 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewerLinkService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ProjectMemberRelationRepository projectMemberRelationRepository;

    @Value("${app.viewer.base-url:https://yaldi.kr}")
    private String baseUrl;

    /**
     * Redis 키 접두사 상수
     * - Forward Mapping: projectKey → linkId 조회용
     * - Reverse Mapping: linkId → projectKey 조회용
     */
    private static final String REDIS_KEY_PREFIX_FORWARD = "viewer:link:";
    private static final String REDIS_KEY_PREFIX_REVERSE = "viewer:reverse:";

    private static final long LINK_TTL_DAYS = 3;
    private static final long LINK_TTL_SECONDS = LINK_TTL_DAYS * 24 * 60 * 60; // 259200초

    public ViewerLinkResponse getOrCreateViewerLink(Long projectKey) {
        // Forward Key 생성: "viewer:link:{projectKey}"
        String redisKey = buildRedisKey(projectKey);

        // Redis에서 linkId 조회 (Forward Mapping 사용)
        String linkId = redisTemplate.opsForValue().get(redisKey);

        if (linkId != null) {
            Long remainingTtl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

            if (remainingTtl != null && remainingTtl > 0) {
                log.info("기존 뷰어링크 반환 - ProjectKey: {}, LinkId: {}, TTL: {}초", projectKey, linkId, remainingTtl);
                return ViewerLinkResponse.of(linkId, baseUrl, remainingTtl);
            }
        }

        linkId = UUID.randomUUID().toString();

        // Redis에 Forward + Reverse 매핑 저장 (3일 TTL)
        createWithReverseMapping(projectKey, linkId);

        log.info("새 뷰어링크 생성 - ProjectKey: {}, LinkId: {}, TTL: {}초", projectKey, linkId, LINK_TTL_SECONDS);

        return ViewerLinkResponse.of(linkId, baseUrl, LINK_TTL_SECONDS);
    }

    /**
     * 뷰어가 링크를 클릭했을 때 호출됨
     * linkId만 알고 있는 상태에서 어떤 프로젝트인지 찾기위함
     *
     * 1. Reverse Key로 조회: "viewer:reverse:{linkId}" → projectKey
     * 2. projectKey 발견 → 유효한 링크
     * 3. 없으면 → 만료되었거나 잘못된 링크
     */
    public ViewerLinkInfo validateAndGetLinkInfo(String linkId) {
        // Reverse Key 생성
        String reverseKey = buildReverseKey(linkId);

        // Redis에서 projectKey 조회 (Reverse Mapping 사용)
        String projectKeyStr = redisTemplate.opsForValue().get(reverseKey);

        if (projectKeyStr == null) {
            log.warn("유효하지 않은 뷰어링크 - LinkId: {}", linkId);
            throw new GeneralException(ErrorStatus.INVALID_VIEWER_LINK);
        }

        try {
            Long projectKey = Long.parseLong(projectKeyStr);
            log.info("뷰어링크 검증 성공 - LinkId: {}, ProjectKey: {}", linkId, projectKey);
            return new ViewerLinkInfo(projectKey);
        } catch (NumberFormatException e) {
            log.error("뷰어링크 데이터 손상 - LinkId: {}, 저장된 값: {}", linkId, projectKeyStr, e);
            throw new GeneralException(ErrorStatus.INVALID_VIEWER_LINK);
        }
    }

    public boolean isProjectMember(Long projectKey, Integer userKey) {
        return projectMemberRelationRepository.existsByProjectKeyAndMemberKey(projectKey, userKey);
    }

    /**
     * Forward + Reverse 매핑을 동시에 생성
     */
    private void createWithReverseMapping(Long projectKey, String linkId) {
        String forwardKey = buildForwardKey(projectKey);
        String reverseKey = buildReverseKey(linkId);
        String projectKeyStr = projectKey.toString();

        // Forward: viewer:link:{projectKey} → linkId
        redisTemplate.opsForValue().set(forwardKey, linkId, LINK_TTL_SECONDS, TimeUnit.SECONDS);

        // Reverse: viewer:reverse:{linkId} → projectKey
        redisTemplate.opsForValue().set(reverseKey, projectKeyStr, LINK_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Forward Key 생성: "viewer:link:{projectKey}"
     */
    private String buildForwardKey(Long projectKey) {
        return REDIS_KEY_PREFIX_FORWARD + projectKey;
    }

    /**
     * Reverse Key 생성: "viewer:reverse:{linkId}"
     */
    private String buildReverseKey(String linkId) {
        return REDIS_KEY_PREFIX_REVERSE + linkId;
    }

    /**
     * @deprecated buildForwardKey()를 사용하세요
     */
    @Deprecated
    private String buildRedisKey(Long projectKey) {
        return buildForwardKey(projectKey);
    }
}
