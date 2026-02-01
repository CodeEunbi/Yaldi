package com.yaldi.domain.version.listener;

import com.yaldi.domain.version.client.VersionAiClient;
import com.yaldi.domain.version.dto.kafka.MockDataCreateMessage;
import com.yaldi.domain.version.entity.MockData;
import com.yaldi.domain.version.repository.MockDataRepository;
import com.yaldi.global.asyncjob.entity.AsyncJob;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import com.yaldi.global.asyncjob.service.AsyncJobService;
import com.yaldi.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataKafkaConsumerListener {

    private final VersionAiClient versionAiClient;
    private final S3Service s3Service;
    private final MockDataRepository mockDataRepository;
    private final AsyncJobService asyncJobService;

    @KafkaListener(
            topics = "yaldi.mockdata.create",
            groupId = "yaldi-mockdata-group",
            containerFactory = "mockDataKafkaListenerContainerFactory"
    )
    public void consumeMockDataGenerateRequest(MockDataCreateMessage message) {
        log.info("Mock 데이터 생성 요청 수신 - JobId: {}, VersionKey: {}, RowCount: {}",
                message.jobId(), message.versionKey(), message.rowCount());

        try {
            AsyncJob asyncJob = asyncJobService.updateStatus(message.jobId(), AsyncJobStatus.PROCESSING);

            // AsyncService에서 생성된  MockData 조회
            MockData mockData = mockDataRepository.findByAsyncJob(asyncJob)
                    .orElseThrow(() -> new RuntimeException("MockData not found for jobId: " + message.jobId()));

            // AI 서버 호출
            log.info("AI 서버에 SQL 생성 요청 중...");
            String sqlContent = versionAiClient.createSql(
                    message.schemaData(),
                    message.rowCount()
            );

            String fileName = String.format("mock_data_%s_%d.sql",
                    message.versionName().replace(".", "_"),
                    System.currentTimeMillis()
            );

            String s3Url = s3Service.uploadFile("mock-data", fileName, sqlContent);

            mockData.complete(fileName, s3Url);
            mockDataRepository.save(mockData);

            asyncJobService.updateStatus(message.jobId(), AsyncJobStatus.COMPLETED);

            log.info("Mock 데이터 생성 완료 - JobId: {}, MockDataKey: {}, S3 URL: {}",
                    message.jobId(), mockData.getMockDataKey(), s3Url);

            // TODO: 완료 알림 전송
        } catch (Exception e) {
            log.error("Mock 데이터 생성 실패 - JobId: {}, Error: {}",
                    message.jobId(), e.getMessage(), e);

            // 상태 업데이트: FAILED
            asyncJobService.failJob(message.jobId(), e.getMessage());

            // TODO: 실패 알림 전송
        }
    }
}
