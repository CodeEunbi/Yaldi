package com.yaldi.domain.consultation.validator;

import com.yaldi.domain.project.entity.Project;
import com.yaldi.domain.project.repository.ProjectRepository;
import com.yaldi.domain.project.service.ProjectAccessValidator;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Consultation 도메인의 검증 로직을 담당하는 Validator
 * - 프로젝트 존재 및 삭제 여부 확인
 * - 프로젝트 접근 권한 확인
 */
@Component
@RequiredArgsConstructor
public class ConsultationValidator {

    private final ProjectRepository projectRepository;
    private final ProjectAccessValidator projectAccessValidator;

    /**
     * 프로젝트 존재 여부, 삭제 여부, 접근 권한을 모두 확인
     *
     * @param projectKey 프로젝트 키
     * @param userKey    사용자 키
     * @return 검증된 프로젝트
     * @throws GeneralException 프로젝트가 없거나 삭제되었거나 권한이 없는 경우
     */
    public Project validateProjectAccess(Long projectKey, Integer userKey) {
        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        if (project.getDeletedAt() != null) {
            throw new GeneralException(ErrorStatus.PROJECT_NOT_FOUND);
        }

        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        return project;
    }
}
