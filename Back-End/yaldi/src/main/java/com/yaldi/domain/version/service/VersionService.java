package com.yaldi.domain.version.service;

import com.yaldi.domain.project.entity.Project;
import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.domain.project.repository.ProjectRepository;
import com.yaldi.domain.version.dto.kafka.VersionProcessingMessage;
import com.yaldi.domain.version.dto.request.CreateVersionRequest;
import com.yaldi.domain.version.dto.request.UpdateVersionRequest;
import com.yaldi.domain.version.dto.request.UpdateVersionVisibilityRequest;
import com.yaldi.domain.version.dto.response.VersionListResponse;
import com.yaldi.domain.version.dto.response.VersionResponse;
import com.yaldi.domain.version.entity.Version;
import com.yaldi.domain.version.repository.VersionRepository;
import com.yaldi.global.asyncjob.entity.AsyncJob;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.global.asyncjob.service.AsyncJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private static final String JOB_TYPE_VERSION_VERIFICATION = "VERSION_VERIFICATION";

    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRelationRepository projectMemberRelationRepository;
    private final AsyncJobService asyncJobService;
    private final VersionProcessingProducerService verificationProducerService;
    private final VersionRollbackService rollbackService;

    @Transactional
    public VersionResponse createVersion(Integer userKey, Long projectKey, CreateVersionRequest request) {
        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        validateProjectMember(userKey, projectKey);

        Version version = Version.builder()
                .projectKey(projectKey)
                .name(request.name())
                .description(request.description() != null ? request.description() : "")
                .schemaData(request.schemaData())
                .isPublic(request.isPublic() != null ? request.isPublic() : false)
                .build();

        version = versionRepository.save(version);

        log.info("Version created: versionKey={}, projectKey={}, name={}, status={}",
                version.getVersionKey(), projectKey, version.getName(), version.getDesignVerificationStatus());

        AsyncJob asyncJob = asyncJobService.createJob(
                JOB_TYPE_VERSION_VERIFICATION,
                userKey,
                version.getVersionKey()
        );

        version.updateAsyncJob(asyncJob);
        version = versionRepository.save(version);

        // Kafka 메시지 발행
        VersionProcessingMessage message = new VersionProcessingMessage(
                asyncJob.getJobId(),
                version.getVersionKey(),
                projectKey,
                project.getName(),
                project.getDescription() != null ? project.getDescription() : "",
                project.getImageUrl(),
                version.getName(),
                version.getDescription() != null ? version.getDescription() : "",
                version.getSchemaData()
        );

        verificationProducerService.publishVersionVerificationRequest(message);

        return VersionResponse.from(version);
    }

    @Transactional(readOnly = true)
    public Page<VersionListResponse> getVersions(Integer userKey, Long projectKey, int page) {
        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        validateProjectMember(userKey, projectKey);

        Pageable pageable = PageRequest.of(page, 10);
        Page<Version> versions = versionRepository.findByProjectKeyOrderByVersionKeyDesc(projectKey, pageable);

        return versions.map(VersionListResponse::from);
    }

    @Transactional(readOnly = true)
    public VersionResponse getVersion(Integer userKey, Long versionKey) {
        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = version.getProjectKey();
        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        validateProjectMember(userKey, projectKey);

        return VersionResponse.from(version);
    }

    @Transactional
    public VersionResponse updateVersion(Integer userKey, Long versionKey, UpdateVersionRequest request) {
        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = version.getProjectKey();
        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        validateProjectMember(userKey, projectKey);

        if (request.name() != null && !request.name().isBlank()) version.updateName(request.name());
        if (request.description() != null) version.updateDescription(request.description());

        Version updatedVersion = versionRepository.save(version);

        return VersionResponse.from(updatedVersion);
    }

    /**
     * 버전 공개 여부 수정 (Public/Private 토글)
     */
    @Transactional
    public VersionResponse updateVisibility(Integer userKey, Long versionKey, UpdateVersionVisibilityRequest request) {
        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = version.getProjectKey();
        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        validateProjectMember(userKey, projectKey);

        // Public/Private 설정
        if (request.isPublic()) {
            version.makePublic();
        } else {
            version.makePrivate();
        }

        Version updatedVersion = versionRepository.save(version);

        log.info("Version visibility updated: versionKey={}, isPublic={}",
                updatedVersion.getVersionKey(), updatedVersion.getIsPublic());

        return VersionResponse.from(updatedVersion);
    }

    //프로젝트의 Public 버전 리스트 조회 (권한 확인 없음)
    @Transactional(readOnly = true)
    public List<VersionListResponse> getPublicVersions(Long projectKey) {

        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        List<Version> versions = versionRepository.findByProjectKeyAndIsPublicTrueOrderByCreatedAtDesc(projectKey);

        return versions.stream()
                .map(VersionListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Public 버전 상세 조회 (권한 확인 없음)
     * 검색 결과에서 버전 선택 시 해당 버전의 ERD 정보(version의 jsonb)를 조회
     */
    @Transactional(readOnly = true)
    public VersionResponse getPublicVersion(Long projectKey, Long versionKey) {

        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        if (!version.getProjectKey().equals(projectKey)) {
            throw new GeneralException(ErrorStatus.VERSION_NOT_FOUND);
        }

        if (!version.getIsPublic()) {
            throw new GeneralException(ErrorStatus.VERSION_FORBIDDEN);
        }

        return VersionResponse.from(version);
    }

    private void validateProjectMember(Integer userKey, Long projectKey) {
        if (!projectMemberRelationRepository.existsByProjectKeyAndMemberKey(projectKey, userKey)) {
            throw new GeneralException(ErrorStatus.PROJECT_FORBIDDEN);
        }
    }

    @Transactional
    public VersionResponse rollbackToVersion(Integer userKey, Long versionKey) {
        Version version = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = version.getProjectKey();

        validateProjectMember(userKey, projectKey);

        Map<String, Object> schemaData = version.getSchemaData();

        // VersionRollbackService에 위임
        rollbackService.rollbackErdToSnapshot(projectKey, schemaData);

        log.info("ERD 롤백 완료 - projectKey={}, versionKey={}", projectKey, versionKey);
        return VersionResponse.from(version);
    }
}
