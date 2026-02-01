package com.yaldi.domain.search.service;

import com.yaldi.domain.search.document.VersionDocument;
import com.yaldi.domain.search.repository.VersionSearchRepository;
import com.yaldi.domain.version.entity.Version;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class VersionSearchSyncService {

    private final VersionSearchRepository versionSearchRepository;

    @Transactional(readOnly = true)
    public void syncToElasticsearch(Version version, String projectName, String projectDescription, String projectImageUrl) {
        try {
            // 벡터 문자열을 float 배열로 변환
            float[] vectorArray = parseVector(version.getVector());

            VersionDocument document = VersionDocument.builder()
                    .versionKey(version.getVersionKey())
                    .projectKey(version.getProjectKey())
                    .versionName(version.getName())
                    .versionDescription(version.getDescription())
                    .projectName(projectName)
                    .projectDescription(projectDescription)
                    .projectImageUrl(projectImageUrl)
                    .vector(vectorArray)
                    .isPublic(version.getIsPublic())
                    .designVerificationStatus(version.getDesignVerificationStatus().getValue())
                    .createdAt(version.getCreatedAt())
                    .build();

            versionSearchRepository.save(document);
            log.info("Version synced to Elasticsearch: versionKey={}, projectName={}",version.getVersionKey(), projectName);
        } catch (Exception e) {
            log.error("Failed to sync version to Elasticsearch: versionKey={}",version.getVersionKey(), e);
            // Elasticsearch 동기화 실패는 원본 데이터에 영향 없도록 예외를 삼킴
        }
    }

    private float[] parseVector(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            log.warn("Vector is null or empty, returning zero vector");
            return new float[1536]; // 기본값 (0으로 채워진 벡터)
        }

        try {
            // "[0.1, 0.2, ...]" 형식 파싱
            String cleaned = vectorString.replace("[", "").replace("]", "").trim();
            String[] parts = cleaned.split(",");
            float[] result = new float[parts.length];

            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }

            if (result.length != 1536) {
                log.warn("벡터 차원 mismatch got {}", result.length);
            }

            return result;
        } catch (NumberFormatException e) {
            log.error("Failed to parse vector string: {}", vectorString, e);
            return new float[1536]; // 파싱 실패 시 기본값 반환
        }
    }
}
