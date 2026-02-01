package com.yaldi.domain.project.service;

import com.yaldi.domain.project.entity.ProjectMemberRole;
import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 프로젝트 접근 권한 검증 유틸리티
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectAccessValidator {

    private final ProjectMemberRelationRepository projectMemberRelationRepository;

    /**
     * 사용자가 프로젝트 멤버인지 검증
     *
     * @param projectKey 프로젝트 ID
     * @param userKey 사용자 ID
     * @throws GeneralException 멤버가 아닌 경우 PROJECT_FORBIDDEN
     */
    public void validateProjectAccess(Long projectKey, Integer userKey) {
        if (!projectMemberRelationRepository.existsByProjectKeyAndMemberKey(projectKey, userKey)) {
            log.warn("Unauthorized project access attempt: projectKey={}, userKey={}", projectKey, userKey);
            throw new GeneralException(ErrorStatus.PROJECT_FORBIDDEN);
        }
    }

    /**
     * 사용자가 프로젝트 멤버인지 확인 (예외 발생 없음)
     *
     * @param projectKey 프로젝트 ID
     * @param userKey 사용자 ID
     * @return 멤버 여부
     */
    public boolean isProjectMember(Long projectKey, Integer userKey) {
        return projectMemberRelationRepository.existsByProjectKeyAndMemberKey(projectKey, userKey);
    }

    /**
     * 프로젝트 멤버의 역할 조회 (Redis 캐싱 적용)
     *
     * <p>JWT에는 최소한의 정보(userId, email)만 포함하고, 권한은 매번 조회합니다.
     * 하지만 매 요청마다 DB 조회하면 병목 현상이 발생하므로 Redis 캐싱을 적용합니다.</p>
     *
     * <h3>캐싱 전략:</h3>
     * <ul>
     *   <li><strong>Cache Name:</strong> project:member:role</li>
     *   <li><strong>Cache Key:</strong> projectKey:userKey (예: 123:456)</li>
     *   <li><strong>TTL:</strong> 5분 (권한 변경 시 최대 5분 후 반영)</li>
     *   <li><strong>Eviction:</strong> 권한 변경 시 수동으로 캐시 무효화 필요 (@CacheEvict)</li>
     * </ul>
     *
     * <h3>성능 개선:</h3>
     * <pre>
     * [Before - DB 매번 조회]
     * WebSocket 메시지 100건 수신 시:
     * → DB Connection 100회 발생
     *
     * [After - Redis 캐싱]
     * WebSocket 메시지 100건 수신 시:
     * → DB Connection 1회 발생 (첫 요청만)
     * → 나머지 99건은 Redis에서 조회 (평균 0.5ms)
     * </pre>
     *
     * @param projectKey 프로젝트 ID
     * @param userKey 사용자 ID
     * @return 프로젝트 멤버 역할 (OWNER, EDITOR, ADMIN)
     * @throws GeneralException 프로젝트 멤버가 아닌 경우 PROJECT_FORBIDDEN
     */
    @Cacheable(
        value = "project:member:role",
        key = "#projectKey + ':' + #userKey",
        unless = "#result == null"
    )
    public ProjectMemberRole getMemberRole(Long projectKey, Integer userKey) {
        return projectMemberRelationRepository.findByProjectKeyAndMemberKey(projectKey, userKey)
                .map(relation -> relation.getRole())
                .orElseThrow(() -> {
                    log.warn("User is not a member of project: projectKey={}, userKey={}", projectKey, userKey);
                    return new GeneralException(ErrorStatus.PROJECT_FORBIDDEN);
                });
    }
}
