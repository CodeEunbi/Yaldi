package com.yaldi.domain.version.service;

import com.yaldi.domain.project.entity.ProjectMemberRelation;
import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.domain.version.entity.DesignVerificationStatus;
import com.yaldi.domain.version.dto.kafka.MockDataCreateMessage;
import com.yaldi.domain.version.dto.request.MockDataCreateRequest;
import com.yaldi.domain.version.dto.response.MockDataResponse;
import com.yaldi.domain.version.entity.MockData;
import com.yaldi.domain.version.entity.Version;
import com.yaldi.domain.version.repository.MockDataRepository;
import com.yaldi.domain.version.repository.VersionRepository;
import com.yaldi.global.asyncjob.entity.AsyncJob;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.global.asyncjob.service.AsyncJobService;
import com.yaldi.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataService {

    private static final String JOB_TYPE_MOCK_DATA = "MOCK_DATA";

    private final VersionRepository versionRepository;
    private final ProjectMemberRelationRepository projectMemberRelationRepository;
    private final MockDataRepository mockDataRepository;
    private final MockDataProducerService mockDataProducerService;
    private final AsyncJobService asyncJobService;
    private final S3Service s3Service;


    @Transactional
    public MockDataResponse createMockData(
            Integer userKey,
            Long versionKey,
            MockDataCreateRequest request
    ) {
        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = version.getProjectKey();
        validatePermission(userKey, projectKey);

        if (version.getDesignVerificationStatus() != DesignVerificationStatus.SUCCESS) throw new GeneralException(ErrorStatus.VERSION_NOT_VERIFIED);

        // 비동기 작업 생성 //MockData 먼저 생성 : fileName/filePath는 null, Consumer가 채움
        AsyncJob asyncJob = asyncJobService.createJob(JOB_TYPE_MOCK_DATA, userKey, versionKey);

        MockData mockData = MockData.builder()
                .asyncJob(asyncJob)
                .versionKey(versionKey)
                .fileName(null)  // Consumer에서 UPDATE
                .filePath(null)  // Consumer에서 UPDATE
                .rowCounts(request.rowCount().shortValue())
                .build();

        MockData savedMockData = mockDataRepository.save(mockData);

        MockDataCreateMessage message = new MockDataCreateMessage(
                asyncJob.getJobId(),
                userKey,
                versionKey,
                version.getName(),
                version.getSchemaData(),
                request.rowCount()
        );

        mockDataProducerService.publishMockDataCreateRequest(message);

        return new MockDataResponse(
                savedMockData.getMockDataKey(),
                AsyncJobStatus.PENDING,
                versionKey,
                request.rowCount(),
                null,  // Consumer에서 UPDATE
                null   // downloadUrl (조회 시 생성)
        );
    }

    @Transactional(readOnly = true)
    public List<MockDataResponse> getMockDataList( Integer userKey, Long versionKey ) {
        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = version.getProjectKey();
        validatePermission(userKey, projectKey);

        List<MockData> mockDataList = mockDataRepository.findByVersionKeyOrderByCreatedAtDesc(versionKey);

        return mockDataList.stream()
                .filter(mockData -> mockData.getAsyncJob().getStatus() == AsyncJobStatus.COMPLETED)
                .map(mockData -> new MockDataResponse(
                        mockData.getMockDataKey(),
                        mockData.getAsyncJob().getStatus(),
                        mockData.getVersionKey(),
                        mockData.getRowCounts().intValue(),
                        mockData.getFileName(),
                        null  // List 조회 시에는 downloadUrl 생성 안 함
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MockDataResponse getMockData(Integer userKey, Long versionKey, Long mockDataKey) {
        MockData mockData = mockDataRepository.findById(mockDataKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOCK_DATA_NOT_FOUND));

        if (!mockData.getVersionKey().equals(versionKey)) {
            throw new GeneralException(ErrorStatus.VERSION_MOCK_DATA_MISMATCH);
        }

        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = version.getProjectKey();
        validatePermission(userKey, projectKey);

        AsyncJob asyncJob = mockData.getAsyncJob();

        String downloadUrl = null;
        if (asyncJob.getStatus() == AsyncJobStatus.COMPLETED && mockData.getFilePath() != null) {
            downloadUrl = s3Service.generatePresignedUrl(
                    mockData.getFilePath(),
                    Duration.ofHours(1)  // 1시간 유효
            );
        }

        return new MockDataResponse(
                mockData.getMockDataKey(),
                asyncJob.getStatus(),
                mockData.getVersionKey(),
                mockData.getRowCounts().intValue(),
                mockData.getFileName(),
                downloadUrl
        );
    }

    private void validatePermission(Integer userKey, Long projectKey) {
        ProjectMemberRelation memberRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));
    }
}
