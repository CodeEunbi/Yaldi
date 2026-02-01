package com.yaldi.domain.project.service;

import com.yaldi.domain.project.dto.request.CreateProjectRequest;
import com.yaldi.domain.project.dto.response.ProjectResponse;
import com.yaldi.domain.project.dto.request.UpdateProjectRequest;
import com.yaldi.domain.project.entity.Project;
import com.yaldi.domain.project.entity.ProjectMemberRelation;
import com.yaldi.domain.project.entity.ProjectMemberRole;
import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.domain.project.repository.ProjectRepository;
import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.repository.TeamRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 프로젝트 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRelationRepository projectMemberRelationRepository;
    private final TeamRepository teamRepository;
    private final UserTeamRelationRepository userTeamRelationRepository;

    /**
     * 프로젝트 생성
     *
     * @param userKey 생성하는 사용자 ID
     * @param request 프로젝트 생성 요청
     * @return 생성된 프로젝트 정보
     */
    @Transactional
    public ProjectResponse createProject(Integer userKey, CreateProjectRequest request) {
        // 팀 존재 여부 확인
        Team team = teamRepository.findActiveTeamById(request.teamKey())
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_NOT_FOUND));

        // 사용자가 해당 팀의 멤버인지 확인
        if (!userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(userKey, request.teamKey())) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }

        // 프로젝트 생성
        Project project = Project.builder()
                .teamKey(request.teamKey())
                .name(request.name())
                .description(request.description() != null ? request.description() : "")
                .imageUrl(request.imageUrl())
                .build();

        project = projectRepository.save(project);

        // 프로젝트 생성자를 OWNER로 추가
        ProjectMemberRelation ownerRelation = ProjectMemberRelation.builder()
                .projectKey(project.getProjectKey())
                .memberKey(userKey)
                .role(ProjectMemberRole.OWNER)
                .build();

        projectMemberRelationRepository.save(ownerRelation);

        // 팀 소유자를 프로젝트에 ADMIN으로 자동 추가 (생성자와 다른 경우에만)
        Integer teamOwnerId = team.getOwner().getUserKey();
        if (!teamOwnerId.equals(userKey)) {
            ProjectMemberRelation adminRelation = ProjectMemberRelation.builder()
                    .projectKey(project.getProjectKey())
                    .memberKey(teamOwnerId)
                    .role(ProjectMemberRole.ADMIN)
                    .build();

            projectMemberRelationRepository.save(adminRelation);

            log.info("Team owner added as ADMIN: projectKey={}, teamOwner={}",
                    project.getProjectKey(), teamOwnerId);
        }

        log.info("Project created: projectKey={}, name={}, owner={}",
                project.getProjectKey(), project.getName(), userKey);

        return ProjectResponse.from(project, ProjectMemberRole.OWNER);
    }

    /**
     * 프로젝트 조회 (단건)
     *
     * @param userKey 조회하는 사용자 ID
     * @param projectKey 프로젝트 ID
     * @return 프로젝트 정보
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Integer userKey, Long projectKey) {
        // 프로젝트 존재 여부 확인
        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        // 사용자의 프로젝트 멤버십 확인
        ProjectMemberRelation memberRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        return ProjectResponse.from(project, memberRelation.getRole());
    }

    /**
     * 팀의 모든 프로젝트 조회 (페이징)
     *
     * @param userKey 조회하는 사용자 ID
     * @param teamKey 팀 ID
     * @param pageable 페이징 정보
     * @return 프로젝트 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getProjectsByTeam(Integer userKey, Integer teamKey, Pageable pageable) {
        // 팀 존재 여부 확인
        teamRepository.findActiveTeamById(teamKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_NOT_FOUND));

        // 사용자가 해당 팀의 멤버인지 확인
        if (!userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(userKey, teamKey)) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }

        Page<Project> projectsPage = projectRepository.findByTeamKey(teamKey, pageable);

        return projectsPage.map(project -> {
            // 각 프로젝트에서 사용자의 역할 조회
            ProjectMemberRole role = projectMemberRelationRepository
                    .findByProjectKeyAndMemberKey(project.getProjectKey(), userKey)
                    .map(ProjectMemberRelation::getRole)
                    .orElse(null);

            return ProjectResponse.from(project, role);
        });
    }

    /**
     * 사용자가 속한 모든 프로젝트 조회 (페이징, 최근 활동순)
     *
     * @param userKey 사용자 ID
     * @param pageable 페이징 정보 (lastActivityAt 기준 정렬 가능)
     * @return 프로젝트 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getMyProjects(Integer userKey, Pageable pageable) {
        // Project를 직접 페이징하여 lastActivityAt 정렬 적용
        Page<Project> projectsPage = projectRepository.findByMemberKey(userKey, pageable);

        // 각 프로젝트에 대한 사용자 역할 조회 (배치 조회)
        List<Long> projectKeys = projectsPage.getContent().stream()
                .map(Project::getProjectKey)
                .collect(Collectors.toList());

        // 해당 사용자의 프로젝트별 역할만 조회
        java.util.Map<Long, ProjectMemberRole> roleMap = new java.util.HashMap<>();
        if (!projectKeys.isEmpty()) {
            List<ProjectMemberRelation> relations = projectMemberRelationRepository
                    .findByMemberKeyAndProjectKeyIn(userKey, projectKeys);

            for (ProjectMemberRelation relation : relations) {
                roleMap.put(relation.getProjectKey(), relation.getRole());
            }
        }

        return projectsPage.map(project -> {
            ProjectMemberRole role = roleMap.get(project.getProjectKey());
            return ProjectResponse.from(project, role);
        });
    }

    /**
     * 프로젝트 수정
     *
     * @param userKey 수정하는 사용자 ID
     * @param projectKey 프로젝트 ID
     * @param request 프로젝트 수정 요청
     * @return 수정된 프로젝트 정보
     */
    @Transactional
    public ProjectResponse updateProject(Integer userKey, Long projectKey, UpdateProjectRequest request) {
        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        // 사용자의 권한 확인 (OWNER 또는 ADMIN만 수정 가능)
        ProjectMemberRelation memberRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        if (memberRelation.getRole() != ProjectMemberRole.OWNER &&
            memberRelation.getRole() != ProjectMemberRole.ADMIN) {
            throw new GeneralException(ErrorStatus.PROJECT_PERMISSION_DENIED);
        }

        // 프로젝트 정보 업데이트
        if (request.name() != null) {
            project.updateName(request.name());
        }
        if (request.description() != null) {
            project.updateDescription(request.description());
        }
        if (request.imageUrl() != null) {
            project.updateImageUrl(request.imageUrl());
        }

        project = projectRepository.save(project);

        log.info("Project updated: projectKey={}, updatedBy={}", projectKey, userKey);

        return ProjectResponse.from(project, memberRelation.getRole());
    }

    /**
     * 프로젝트 삭제 (Soft Delete)
     *
     * @param userKey 삭제하는 사용자 ID
     * @param projectKey 프로젝트 ID
     */
    @Transactional
    public void deleteProject(Integer userKey, Long projectKey) {
        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        // 사용자의 권한 확인 (OWNER만 삭제 가능)
        ProjectMemberRelation memberRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        if (memberRelation.getRole() != ProjectMemberRole.OWNER) {
            throw new GeneralException(ErrorStatus.PROJECT_PERMISSION_DENIED);
        }

        // Soft Delete
        project.softDelete();
        projectRepository.save(project);

        log.info("Project soft deleted: projectKey={}, deletedBy={}", projectKey, userKey);
    }

    /**
     * 프로젝트 강제 삭제 (Hard Delete)
     *
     * <p>즉시 물리적으로 삭제합니다. 복구 불가능합니다.</p>
     * <p>관리자 또는 테스트 용도로만 사용해야 합니다.</p>
     * <p>DB의 ON DELETE CASCADE 덕분에 관련 데이터가 자동으로 삭제됩니다.</p>
     *
     * @param userKey 삭제하는 사용자 ID
     * @param projectKey 프로젝트 ID
     */
    @Transactional
    public void forceDeleteProject(Integer userKey, Long projectKey) {
        // 삭제된 프로젝트도 조회할 수 있어야 함 (soft deleted 프로젝트도 강제 삭제 가능)
        Project project = projectRepository.findByIdIncludingDeleted(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        // 사용자의 권한 확인 (OWNER만 삭제 가능)
        ProjectMemberRelation memberRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        if (memberRelation.getRole() != ProjectMemberRole.OWNER) {
            throw new GeneralException(ErrorStatus.PROJECT_PERMISSION_DENIED);
        }

        // Hard Delete
        // DB의 ON DELETE CASCADE 덕분에 관련된 모든 데이터가 자동으로 삭제됨:
        // - project_member_relations (CASCADE)
        // - 그 외 soft delete 데이터는 남아있지만 스케줄러가 정리함
        projectRepository.delete(project);

        log.warn("Project FORCE deleted (HARD DELETE): projectKey={}, deletedBy={}", projectKey, userKey);
    }
}
