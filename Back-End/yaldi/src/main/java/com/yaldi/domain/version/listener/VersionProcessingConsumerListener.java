package com.yaldi.domain.version.listener;

import com.yaldi.domain.search.service.VersionSearchSyncService;
import com.yaldi.domain.version.client.GraphRagAiClient;
import com.yaldi.domain.version.client.VersionAiClient;
import com.yaldi.domain.version.dto.kafka.VersionProcessingMessage;
import com.yaldi.domain.version.dto.response.VersionVerificationResult;
import com.yaldi.domain.version.entity.DesignVerificationStatus;
import com.yaldi.domain.version.entity.Version;
import com.yaldi.domain.version.repository.VersionRepository;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import com.yaldi.global.asyncjob.service.AsyncJobService;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//버전 검증 Kafka Consumer
@Slf4j
@Component
@RequiredArgsConstructor
public class VersionProcessingConsumerListener {

    private final VersionAiClient aiClient;
    private final GraphRagAiClient graphRagAiClient;
    private final VersionRepository versionRepository;
    private final AsyncJobService asyncJobService;
    private final VersionSearchSyncService versionSearchSyncService;

    @KafkaListener(
            topics = "yaldi.version.verification",
            groupId = "yaldi-version-verification-group",
            containerFactory = "versionProcessingKafkaListenerContainerFactory"
    )
    public void consumeVersionVerificationRequest(VersionProcessingMessage message) {
        log.info("버전 처리 요청 수신 - JobId: {}, VersionKey: {}, VersionName: {}",
                message.jobId(), message.versionKey(), message.versionName());

        Version version = null;
        boolean verificationSuccess = false;
        boolean embeddingSuccess = false;

        try {
            asyncJobService.updateStatus(message.jobId(), AsyncJobStatus.PROCESSING);

            version = versionRepository.findById(message.versionKey())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

            version.updateVerificationStatus(com.yaldi.domain.version.entity.DesignVerificationStatus.RUNNING);
            versionRepository.save(version);

            try {
                log.info("AI 서버에 스키마 검증 요청 중 - VersionKey: {}", message.versionKey());
                VersionVerificationResult result = aiClient.verifySchema(
                        message.schemaData(),
                        message.versionName()
                );

                // 검증 결과 저장
                version.updateVerificationStatus(result.status());

                Map<String, Object> verificationResultMap = new HashMap<>();
                verificationResultMap.put("errors", result.errors());
                verificationResultMap.put("warnings", result.warnings());
                verificationResultMap.put("message", result.message());
                verificationResultMap.put("suggestions", result.suggestions());
                version.updateVerificationResult(verificationResultMap);

                verificationSuccess = true;
                log.info("스키마 검증 완료 - VersionKey: {}, Status: {}",
                        message.versionKey(), result.status());

            } catch (Exception e) {
                log.error("스키마 검증 실패 - VersionKey: {}, 계속 진행합니다.", message.versionKey(), e);
                version.updateVerificationStatus(com.yaldi.domain.version.entity.DesignVerificationStatus.FAILED);

                Map<String, Object> errorResultMap = new HashMap<>();
                errorResultMap.put("errors", List.of("AI 서버 검증 호출 실패: " + e.getMessage()));
                errorResultMap.put("warnings", List.of());
                errorResultMap.put("message", "검증 중 오류가 발생했습니다.");
                errorResultMap.put("suggestions", List.of());
                version.updateVerificationResult(errorResultMap);
            }

            // verification_result 저장
            versionRepository.save(version);

            log.info("여기임");
            log.info(String.valueOf(verificationSuccess));
            log.info(String.valueOf(version.getDesignVerificationStatus() == DesignVerificationStatus.SUCCESS));


            // Graph RAG 인덱싱 (검증 성공 시에만, 복구 가능한 외부 의존성)
            if (verificationSuccess && version.getDesignVerificationStatus() == DesignVerificationStatus.SUCCESS) {
                try {
                    log.info("Neo4j Graph RAG 인덱싱 시작 - VersionKey: {}", message.versionKey());
                    boolean indexingSuccess = graphRagAiClient.indexToGraph(
                            message.versionKey(),
                            message.versionName() != null ? message.versionName() : "",
                            message.versionDescription() != null ? message.versionDescription() : "",
                            message.projectName() != null ? message.projectName() : "",
                            message.projectDescription() != null ? message.projectDescription() : "",
                            message.schemaData(),
                            version.getIsPublic(),
                            version.getDesignVerificationStatus().getValue()
                    );

                    if (indexingSuccess) {
                        log.info("Graph RAG 인덱싱 완료 - VersionKey: {}", message.versionKey());
                    } else {
                        log.warn("Graph RAG 인덱싱 실패했지만 원본 데이터는 정상 처리됨 - VersionKey: {}", message.versionKey());
                    }
                } catch (Exception graphException) {
                    log.error("Graph RAG 인덱싱 중 예외 발생 - VersionKey: {}, 원본 데이터는 정상 처리됨",
                            message.versionKey(), graphException);
                }
            }

            try {
                log.info("AI 서버에 임베딩 생성 요청 중 - VersionKey: {}", message.versionKey());
                List<Double> embeddingVector = aiClient.generateEmbedding(
                        message.versionKey(),
                        message.projectKey(),
                        message.projectName(),
                        message.projectDescription(),
                        message.versionName(),
                        message.versionDescription(),
                        message.schemaData()
                );

                // 임베딩 벡터 저장 (pgvector 형식: "[0.1, 0.2, ...]")
                String vectorString = embeddingVector.toString();
                versionRepository.updateVector(message.versionKey(), vectorString);

                embeddingSuccess = true;
                log.info("임베딩 생성 완료 - VersionKey: {}, Vector dimension: {}",
                        message.versionKey(), embeddingVector.size());

                // Elasticsearch 동기화 (복구 가능한 외부 의존성)
                try {
                    versionRepository.findById(message.versionKey())
                            .ifPresentOrElse(
                                    updatedVersion -> {
                                        versionSearchSyncService.syncToElasticsearch(
                                                updatedVersion,
                                                message.projectName(),
                                                message.projectDescription(),
                                                message.projectImageUrl()
                                        );
                                        log.info("Elasticsearch 동기화 완료 - VersionKey: {}", message.versionKey());
                                    },
                                    () -> log.warn("Elasticsearch 동기화 실패: Version 조회 불가 - VersionKey: {}", message.versionKey())
                            );
                } catch (Exception esException) {
                    log.error("Elasticsearch 동기화 실패 - VersionKey: {}, 원본 데이터는 정상 처리됨", message.versionKey(), esException);
                }

            } catch (Exception e) { // vector는 null로 유지
                log.error("임베딩 생성 실패 - VersionKey: {}, 계속 진행합니다.", message.versionKey(), e);
            }
            asyncJobService.updateStatus(message.jobId(), AsyncJobStatus.COMPLETED);

            log.info("버전 처리 완료 - VersionKey: {}, Verification: {}, Embedding: {}",
                    message.versionKey(),
                    verificationSuccess ? "SUCCESS" : "FAILED",
                    embeddingSuccess ? "SUCCESS" : "FAILED");

        } catch (Exception e) { // Version 조회 등 기본 작업 실패 시에만 전체 실패
            log.error("버전 처리 중 오류 발생 - JobId: {}, VersionKey: {}",
                    message.jobId(), message.versionKey(), e);
            try {
                asyncJobService.updateStatus(message.jobId(), AsyncJobStatus.FAILED);

                if (version != null) {
                    version.updateVerificationStatus(com.yaldi.domain.version.entity.DesignVerificationStatus.FAILED);
                    versionRepository.save(version);
                }
            } catch (Exception updateException) {
                log.error("실패 상태 업데이트 중 오류 발생", updateException);
            }
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}
