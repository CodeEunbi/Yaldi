package com.yaldi.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.yaldi.domain.search.client.SearchAiClient;
import com.yaldi.domain.search.document.VersionDocument;
import com.yaldi.domain.search.dto.response.ProjectSearchResponse;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Version 검색 서비스 - 하이브리드 검색으로 구현
 *
 * - 텍스트 검색(BM25) + 벡터 검색(Semantic)을 결합
 * - BM25: 키워드 기반 매칭 (정확한 단어 찾기)
 * - Semantic: 의미 기반 매칭 (유사한 의미 찾기)
 *
 *  역할
 * - Spring: 쿼리 구성 및 전송
 * - Elasticsearch: 실제 검색 수행 (서버)
 * - AI 서버: 텍스트 → 벡터 변환 (임베딩)
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionSearchService {

    /**
     * 벡터 임베딩 차원 (OpenAI text-embedding-ada-002 모델 기준)
     * - OpenAI의 text-embedding-ada-002 모델은 1536차원 벡터 생성
     * - 다른 임베딩 모델 사용 시 이 값을 변경해야 함
     */
    private static final int EMBEDDING_VECTOR_DIMENSION = 1536;

    // Elasticsearch Java Client - ES 서버와 통신
    private final ElasticsearchClient elasticsearchClient;

    // AI 서버 클라이언트 - 검색 쿼리 임베딩 생성
    private final SearchAiClient searchAiClient;

    /**
     * 전체 프로젝트에서 버전 검색
     * 1. 사용자 입력 → AI 서버 → 임베딩 벡터
     * 2. 텍스트 쿼리 + 벡터 쿼리 생성 -> 하이브리드 검색 위해서
     * 3. Elasticsearch에 쿼리 전송
     * 4. ES 서버에서 검색 실행 (BM25 + 코사인 유사도)
     * 5. 상위 20개 결과 반환
     *
     * - 정확한 키워드 매칭 (BM25) + 의미 기반 검색 (벡터)
     * - 둘의 점수를 결합하여 최적의 결과 도출
     */
    public List<VersionDocument> hybridSearch(String queryText) {
        try {
            log.info("검색 시작 - Query: {}", queryText);

            List<Double> queryEmbedding = generateQueryEmbedding(queryText);
            float[] queryVector = convertToFloatArray(queryEmbedding);

            // 검색 쿼리 생성 ===
            List<Query> shouldQueries = new ArrayList<>();

            // 텍스트 검색 쿼리 - BM25 알고리즘
            shouldQueries.add(textQuery(queryText));

            // 벡터 검색 쿼리 - 코사인 유사도, 저장된 벡터와 검색 벡터의 유사도 계산
            shouldQueries.add(vectorQuery(queryVector));

            // should는 "OR" 조건 - 둘 중 하나라도 매칭되면 점수 부여 ES는 각 쿼리의 점수를 합산하여 최종 순위 결정
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("versions")  // 검색할 인덱스명
                    .size(20)           // 상위 20개만 가져오기
                    .source(src -> src
                            .filter(f -> f
                                    .excludes("vector")  // 검색엔 사용하지만 결과 반환 시엔 불필요 (벡터 배열)
                            )
                    )
                    .query(q -> q
                            .bool(b -> b
                                    // should: BM25 점수 + 벡터 유사도 점수 합산
                                    .should(shouldQueries)
                                    // filter: public 버전만 검색 (점수에 영향 없음)
                                    .filter(publicOnlyFilter())
                            )
                    )
            );

            // Elasticsearch 서버로 쿼리 전송 및 검색 실행 : 실제 검색이 ES 서버에서 실행됨
            // - 모든 문서를 순회하며 점수 계산
            // - BM25 점수 + 코사인 유사도 점수를 합산
            SearchResponse<VersionDocument> response = elasticsearchClient.search(
                    searchRequest,
                    VersionDocument.class
            );

            // Hit 객체에서 실제 VersionDocument 추출
            List<VersionDocument> results = response.hits().hits().stream()
                    .map(Hit::source)  // Hit → VersionDocument
                    .toList();

            log.info("검색 완료 - 결과 개수: {}", results.size());
            return results;

        } catch (Exception e) {
            log.error("검색 실패 - Query: {}", queryText, e);
            throw new GeneralException(ErrorStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * 프로젝트 검색 (버전 검색 후 프로젝트로 그룹화)
     *
     *  puublic 버전들 검색
     *  projectKey로 그룹화 (중복 제거)
     *  각 프로젝트에서 가장 점수 높은 버전 1개 선택
     *  프로젝트 정보만 추출하여 반환
     *
     */
    public List<ProjectSearchResponse> searchProjects(String queryText) {
        try {
            log.info("프로젝트 검색 시작 - Query: {}", queryText);

            // 1. 버전 검색 (public만, 하이브리드)
            List<VersionDocument> versions = hybridSearch(queryText);

            // 2. projectKey로 그룹화 (중복 제거)
            // - ES 검색 결과는 점수 순으로 정렬되어 있음
            // - 같은 프로젝트의 첫 번째 버전 = 가장 점수 높은 버전
            Map<Long, VersionDocument> projectMap = versions.stream()
                    .collect(Collectors.toMap(
                            VersionDocument::getProjectKey,
                            v -> v,
                            (v1, v2) -> v1  // 중복 시 첫 번째 (점수 높은) 선택
                    ));

            // 3. 프로젝트 정보만 추출
            List<ProjectSearchResponse> results = projectMap.values().stream()
                    .map(v -> new ProjectSearchResponse(
                            v.getProjectKey(),
                            v.getProjectName(),
                            v.getProjectDescription(),
                            v.getProjectImageUrl()
                    ))
                    .toList();

            log.info("프로젝트 검색 완료 - Query: {}, 결과 개수: {}", queryText, results.size());
            return results;

        } catch (Exception e) {
            log.error("프로젝트 검색 실패 - Query: {}", queryText, e);
            throw new GeneralException(ErrorStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * 텍스트 기반 검색 쿼리 생성 (BM25 알고리즘)
     *
     * BM25란?
     * - Best Matching 25의 약자
     * - 검색 엔진에서 가장 많이 쓰이는 텍스트 랭킹 알고리즘
     * - TF-IDF를 개선한 확률 기반 랭킹 함수
     *
     * - 검색어 "주문 처리"를 "주문", "처리"로 분리
     * - 각 단어가 문서에 얼마나 등장하는지 계산
     * - 드문 단어일수록 높은 점수 부여
     *
     */
    private Query textQuery(String queryText) {
        return Query.of(q -> q
                .multiMatch(m -> m
                        .query(queryText)
                        // 필드명^가중치 형식
                        .fields("versionName^2", "versionDescription", "projectName^1.5", "projectDescription")
                        .boost(1.0f)  // 전체 쿼리 가중치
                )
        );
    }

    /**
     * 벡터 기반 검색 쿼리 생성 (의미 기반 검색)
     *
     * - 텍스트를 숫자 배열(벡터)로 변환하여 유사도 계산
     * - 의미가 비슷하면 벡터도 비슷함!
     *
     * 코사인 유사도
     * - 두 벡터 사이의 각도로 유사도 측정
     *
     *  Painless 스크립트
     * - Elasticsearch 전용 스크립트 언어
     * - Java와 비슷한 문법
     * - ES 서버에서 실행됨 (Spring 아님!)
     *
     * 1. Spring: 쿼리 구성 + 벡터 전송
     * 2. ES 서버: 모든 문서 순회
     * 3. ES 서버: 각 문서의 vector와 queryVector 비교
     * 4. ES 서버: cosineSimilarity() 함수로 유사도 계산
     * 5. ES 서버: 유사도 높은 순으로 정렬
     *
     */
    private Query vectorQuery(float[] queryVector) {
        // JsonData 변환 작업
        List<Float> vectorList = new ArrayList<>(queryVector.length);
        for (float v : queryVector) {
            vectorList.add(v);  // Boxing: float → Float
        }

        // Script 파라미터 구성
        // ES 서버의 Painless 스크립트에 전달할 파라미터
        Map<String, JsonData> params = new HashMap<>();
        params.put("queryVector", JsonData.of(vectorList));

        // scriptScore 쿼리 생성
        return Query.of(q -> q
                .scriptScore(ss -> ss
                        // 모든 문서를 대상으로 검색
                        .query(Query.of(qq -> qq.matchAll(m -> m)))

                        // Painless 스크립트 정의
                        .script(s -> s
                                // ES 서버에서 실행될 Painless 스크립트
                                // cosineSimilarity: ES 내장 함수
                                // params.queryVector: 위에서 정의한 파라미터
                                .source("cosineSimilarity(params.queryVector, 'vector') + 1.0")
                                .params(params)
                        )
                        // 벡터 쿼리 가중치 (텍스트 쿼리와 균형 조정)
                        .boost(1.0f)
                )
        );
    }

    /**
     * Public 버전만 필터링
     * - filter: 점수에 영향 없이 조건만 검사
     * - isPublic = true인 문서만 검색 대상
     */
    private Query publicOnlyFilter() {
        return Query.of(q -> q
                .term(t -> t
                        .field("isPublic")
                        .value(true)
                )
        );
    }

    /**
     * 검색 쿼리 임베딩 생성
     * - 텍스트를 숫자 벡터로 변환하는 과정
     * - AI 모델이 텍스트의 "의미"를 학습하여 변환
     * - 의미가 비슷한 텍스트는 비슷한 벡터로 변환됨
     */
    private List<Double> generateQueryEmbedding(String queryText) {
        try {
            log.debug("검색 쿼리 임베딩 생성 중 - Query: {}", queryText);

            // AI 서버 호출: POST /api/v1/search/embedding
            List<Double> embedding = searchAiClient.generateSearchEmbedding(queryText);

            log.debug("검색 쿼리 임베딩 생성 완료 - Dimension: {}", embedding.size());
            return embedding;

        } catch (Exception e) {
            // AI 서버 실패해도 검색은 계속 진행 (텍스트 검색만 사용)
            log.error("검색 쿼리 임베딩 생성 실패 - Query: {} 텍스트 검색만 수행합니다", queryText, e);
            return createZeroVector();
        }
    }

    /**
     * Fallback용 제로 벡터 생성
     * - AI 서버 장애 시 임베딩 실패 대응
     * - 0 벡터 사용 시 벡터 검색 점수가 0이 됨
     * - 결과적으로 텍스트 검색(BM25)만 작동
     */
    private List<Double> createZeroVector() {
        List<Double> zeroVector = new ArrayList<>(EMBEDDING_VECTOR_DIMENSION);
        for (int i = 0; i < EMBEDDING_VECTOR_DIMENSION; i++) {
            zeroVector.add(0.0);
        }
        return zeroVector;
    }

    /**
     * 타입 변환: List<Double> → float[]
     *
     * - AI 서버: List<Double> 반환
     * - Elasticsearch: float[] 필요
     * - JsonData 변환 시 float[] 사용
     *
     *  Boxing/Unboxing
     * - Double (객체) → double (primitive) → float (primitive)
     */
    private float[] convertToFloatArray(List<Double> doubles) {
        float[] result = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            result[i] = doubles.get(i).floatValue();  // Unboxing + 타입 변환
        }
        return result;
    }
}
