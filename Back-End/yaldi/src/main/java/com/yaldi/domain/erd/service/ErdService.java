package com.yaldi.domain.erd.service;

import com.yaldi.domain.erd.dto.response.ErdResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ERD 통합 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErdService {

    private final ErdTableService erdTableService;
    private final ErdColumnService erdColumnService;
    private final ErdRelationService erdRelationService;

    /**
     * 프로젝트의 전체 ERD 데이터 조회
     *
     * 성능 최적화:
     * - 수정 전: 1 (tables) + 1 (tables 중복) + N (columns) + 1 (relations) = N+3 쿼리
     * - 수정 후: 1 (tables) + 1 (columns) + 1 (relations) = 3 쿼리
     * - 개선율: 테이블 100개 기준 103번 → 3번 (97% 감소)
     */
    public ErdResponse getErdByProjectKey(Long projectKey) {
        // 1. 테이블 전체 조회 (1번 쿼리)
        var tables = erdTableService.getTablesByProjectKey(projectKey);

        // 2. 프로젝트의 모든 컬럼을 한 번에 조회 (1번 쿼리) - 1+N 방지
        var columns = erdColumnService.getColumnsByProjectKey(projectKey);

        // 3. 관계 전체 조회 (1번 쿼리)
        var relations = erdRelationService.getRelationsByProjectKey(projectKey);

        return ErdResponse.builder()
                .projectKey(projectKey)
                .tables(tables)
                .columns(columns)
                .relations(relations)
                .build();
    }
}
