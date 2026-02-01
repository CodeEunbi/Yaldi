package com.yaldi.domain.project.service;

import com.yaldi.domain.notification.service.NotificationService;
import com.yaldi.domain.project.dto.request.AddProjectMemberRequest;
import com.yaldi.domain.project.dto.request.UpdateProjectMemberRoleRequest;
import com.yaldi.domain.project.dto.response.AddProjectMembersResponse;
import com.yaldi.domain.project.dto.response.ProjectMemberHistoryResponse;
import com.yaldi.domain.project.dto.response.ProjectMemberResponse;
import com.yaldi.domain.project.entity.*;
import com.yaldi.domain.project.repository.ProjectMemberHistoryRepository;
import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.domain.project.repository.ProjectRepository;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 프로젝트 멤버 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRelationRepository projectMemberRelationRepository;
    private final ProjectMemberHistoryRepository projectMemberHistoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 프로젝트 멤버 목록 조회 (페이징)
     *
     * @param userKey 조회하는 사용자 ID
     * @param projectKey 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 멤버 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(Integer userKey, Long projectKey) {
        // 프로젝트 존재 여부 확인
        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        // 사용자의 프로젝트 멤버십 확인
        projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        // 멤버 목록 조회
        List<ProjectMemberRelation> members = projectMemberRelationRepository.findByProjectKey(projectKey);

        // 사용자 정보 조회
        List<Integer> userKeys = members.stream()
                .map(ProjectMemberRelation::getMemberKey)
                .collect(Collectors.toList());

        Map<Integer, User> userMap = userRepository.findAllById(userKeys).stream()
                .collect(Collectors.toMap(User::getUserKey, user -> user));

        return members.stream()
                .map(member -> {
                    User user = userMap.get(member.getMemberKey());
                    if (user != null) {
                        return ProjectMemberResponse.from(member, user.getNickname(), user.getEmail());
                    }
                    return ProjectMemberResponse.from(member);
                })
                .collect(Collectors.toList());
    }

    /**
     * 프로젝트 멤버 추가 (여러 명, 부분 성공 지원)
     *
     * @param userKey 추가하는 사용자 ID (OWNER 또는 ADMIN)
     * @param projectKey 프로젝트 ID
     * @param request 멤버 추가 요청
     * @return 성공/실패 목록을 포함한 응답
     */
    @Transactional
    public AddProjectMembersResponse addProjectMembers(Integer userKey, Long projectKey, AddProjectMemberRequest request) {
        // 프로젝트 존재 여부 확인
        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        // 요청자의 권한 확인 (OWNER 또는 ADMIN만 멤버 추가 가능)
        ProjectMemberRelation requesterRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        if (requesterRelation.getRole() != ProjectMemberRole.OWNER &&
            requesterRelation.getRole() != ProjectMemberRole.ADMIN) {
            throw new GeneralException(ErrorStatus.PROJECT_PERMISSION_DENIED);
        }

        List<ProjectMemberResponse> succeeded = new ArrayList<>();
        List<AddProjectMembersResponse.MemberAddFailure> failed = new ArrayList<>();

        // 각 멤버를 독립적으로 추가 시도
        for (AddProjectMemberRequest.MemberToAdd memberToAdd : request.members()) {
            try {
                ProjectMemberResponse result = addSingleMemberWithTransaction(userKey, projectKey, memberToAdd);
                succeeded.add(result);
            } catch (GeneralException e) {
                failed.add(new AddProjectMembersResponse.MemberAddFailure(
                        memberToAdd.memberKey(),
                        e.getErrorStatus().getCode(),
                        e.getErrorStatus().getMessage()
                ));
                log.warn("Failed to add member: projectKey={}, memberKey={}, error={}",
                        projectKey, memberToAdd.memberKey(), e.getErrorStatus().getCode());
            }
        }

        return new AddProjectMembersResponse(succeeded, failed);
    }

    /**
     * 단일 멤버 추가
     */
    @Transactional
    public ProjectMemberResponse addSingleMemberWithTransaction(Integer userKey, Long projectKey, AddProjectMemberRequest.MemberToAdd memberToAdd) {
        // 추가할 사용자 존재 여부 확인
        User targetUser = userRepository.findById(memberToAdd.memberKey())
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));


        // 이미 멤버인지 확인
        if (projectMemberRelationRepository.existsByProjectKeyAndMemberKey(projectKey, memberToAdd.memberKey())) {
            throw new GeneralException(ErrorStatus.PROJECT_DUPLICATE_MEMBER);
        }

        // 멤버 추가
        ProjectMemberRelation newMember = ProjectMemberRelation.builder()
                .projectKey(projectKey)
                .memberKey(memberToAdd.memberKey())
                .role(memberToAdd.role())
                .build();

        newMember = projectMemberRelationRepository.save(newMember);
        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        // 이력 저장
        ProjectMemberHistory history = ProjectMemberHistory.builder()
                .projectKey(projectKey)
                .actorKey(userKey)
                .targetKey(memberToAdd.memberKey())
                .actionType(ProjectMemberActionType.ADD)
                .build();

        projectMemberHistoryRepository.save(history);
        notificationService.notifyUser(
                userKey,
                ProjectMemberActionType.ADD.getValue(),
                project.getName(),
                null);

        log.info("Project member added: projectKey={}, memberKey={}, role={}, by={}",
                projectKey, memberToAdd.memberKey(), memberToAdd.role(), userKey);

        return ProjectMemberResponse.from(newMember, targetUser.getNickname(), targetUser.getEmail());
    }

    /**
     * 프로젝트 멤버 역할 변경
     *
     * @param userKey 변경하는 사용자 ID (OWNER만 가능)
     * @param projectKey 프로젝트 ID
     * @param memberKey 대상 멤버 ID
     * @param request 역할 변경 요청
     * @return 변경된 멤버 정보
     */
    @Transactional
    public ProjectMemberResponse updateProjectMemberRole(
            Integer userKey,
            Long projectKey,
            Integer memberKey,
            UpdateProjectMemberRoleRequest request
    ) {
        // 요청자의 권한 확인 (OWNER만 역할 변경 가능)
        ProjectMemberRelation requesterRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        if (requesterRelation.getRole() != ProjectMemberRole.OWNER) {
            throw new GeneralException(ErrorStatus.PROJECT_PERMISSION_DENIED);
        }
        if(request.role() == ProjectMemberRole.OWNER){
            throw new GeneralException(ErrorStatus.PROJECT_OWNER_DUPLICATE);
        }

        // 대상 멤버 조회
        ProjectMemberRelation targetRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, memberKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_MEMBER_NOT_FOUND));

        // 자기 자신의 역할은 변경 불가
        if (userKey.equals(memberKey)) {
            throw new GeneralException(ErrorStatus.CHANGE_PROJECT_OWNER_FIRST);
        }

        // 역할 변경
        targetRelation.changeRole(request.role());
        targetRelation = projectMemberRelationRepository.save(targetRelation);

        // 이력 저장
        ProjectMemberHistory history = ProjectMemberHistory.builder()
                .projectKey(projectKey)
                .actorKey(userKey)
                .targetKey(memberKey)
                .actionType(ProjectMemberActionType.ROLE_CHANGED)
                .build();

        projectMemberHistoryRepository.save(history);

        log.info("Project member role updated: projectKey={}, memberKey={}, newRole={}, by={}",
                projectKey, memberKey, request.role(), userKey);

        // 사용자 정보 조회
        User targetUser = userRepository.findById(memberKey).orElse(null);
        if (targetUser != null) {
            return ProjectMemberResponse.from(targetRelation, targetUser.getNickname(), targetUser.getEmail());
        }

        return ProjectMemberResponse.from(targetRelation);
    }

    /**
     * OWNER 위임
     * 
     * @param userKey 오너 ID
     * @param targetKey 오너가 될 ID
     * @param projectKey 프로젝트 ID
     */
    @Transactional
    public ProjectMemberResponse changeOwner(Integer userKey, Integer targetKey, Long projectKey) {
        // 요청자의 권한 확인 (OWNER만 역할 변경 가능)
        ProjectMemberRelation requesterRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        if (requesterRelation.getRole() != ProjectMemberRole.OWNER) {
            throw new GeneralException(ErrorStatus.PROJECT_PERMISSION_DENIED);
        }

        // 자기 자신에게 위임 불가
        if (userKey.equals(targetKey)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        ProjectMemberRelation targetRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, targetKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_MEMBER_NOT_FOUND));

        targetRelation.changeRole(ProjectMemberRole.OWNER);
        requesterRelation.changeRole(ProjectMemberRole.EDITOR);

        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        notificationService.notifyUser(
                targetKey,
                "BE_PROJECT_OWNER",
                project.getName(),
                project.getProjectKey());

        return ProjectMemberResponse.from(targetRelation);
    }


    /**
     * 프로젝트 나가기
     *
     * @param userKey 나가는 사용자 ID
     * @param projectKey 프로젝트 ID
     */
    @Transactional
    public void leaveProject(Integer userKey, Long projectKey) {
        // 멤버 관계 조회
        ProjectMemberRelation memberRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        // OWNER면 위임 먼저
        if(memberRelation.getRole() == ProjectMemberRole.OWNER){
            throw new GeneralException(ErrorStatus.CHANGE_PROJECT_OWNER_FIRST);
        }

        // 멤버 삭제
        projectMemberRelationRepository.delete(memberRelation);

        // 이력 저장
        ProjectMemberHistory history = ProjectMemberHistory.builder()
                .projectKey(projectKey)
                .actorKey(userKey)
                .targetKey(userKey)
                .actionType(ProjectMemberActionType.EXIT)
                .build();

        projectMemberHistoryRepository.save(history);

        log.info("Project member left: projectKey={}, memberKey={}", projectKey, userKey);
    }

    /**
     * 프로젝트 멤버 삭제 (내보내기)
     *
     * @param userKey 삭제하는 사용자 ID (OWNER 또는 ADMIN)
     * @param projectKey 프로젝트 ID
     * @param memberKey 삭제할 멤버 ID
     */
    @Transactional
    public void removeProjectMember(Integer userKey, Long projectKey, Integer memberKey) {

        // 요청자의 권한 확인
        ProjectMemberRelation requesterRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_FORBIDDEN));

        if (requesterRelation.getRole() != ProjectMemberRole.OWNER &&
            requesterRelation.getRole() != ProjectMemberRole.ADMIN) {
            throw new GeneralException(ErrorStatus.PROJECT_PERMISSION_DENIED);
        }

        // 자기 자신을 삭제하는 경우 leaveProject를 사용해야 함
        if (userKey.equals(memberKey)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        // 대상 멤버 조회
        ProjectMemberRelation targetRelation = projectMemberRelationRepository
                .findByProjectKeyAndMemberKey(projectKey, memberKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_MEMBER_NOT_FOUND));

        // 멤버 삭제
        projectMemberRelationRepository.delete(targetRelation);

        // 이력 저장 (강제 추방)
        ProjectMemberHistory history = ProjectMemberHistory.builder()
                .projectKey(projectKey)
                .actorKey(userKey)
                .targetKey(memberKey)
                .actionType(ProjectMemberActionType.EXPULSION)
                .build();

        Project project = projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        notificationService.notifyUser(
                userKey,
                ProjectMemberActionType.EXPULSION.getValue(),
                project.getName(),
                null);
        projectMemberHistoryRepository.save(history);

        log.info("Project member removed: projectKey={}, memberKey={}, by={}",
                projectKey, memberKey, userKey);
    }

    /**
     * 프로젝트 멤버 이력 조회 (페이징)
     *
     * @param userKey 조회하는 사용자 ID
     * @param projectKey 프로젝트 ID
     * @param pageable 페이징 정보
     * @return 멤버 이력 목록 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<ProjectMemberHistoryResponse> getProjectMemberHistory(Long projectKey, Pageable pageable) {
        // 이력 조회 (페이징, 최신순)
        Page<ProjectMemberHistory> historiesPage = projectMemberHistoryRepository.findByProjectKeyOrderByCreatedAtDesc(projectKey, pageable);

        // 사용자 정보 조회
        List<Integer> userKeys = historiesPage.getContent().stream()
                .flatMap(h -> Stream.of(h.getActorKey(), h.getTargetKey()))
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, User> userMap = userRepository.findAllById(userKeys).stream()
                .collect(Collectors.toMap(User::getUserKey, user -> user));

        return historiesPage.map(history -> {
            User actor = userMap.get(history.getActorKey());
            User target = userMap.get(history.getTargetKey());

            return ProjectMemberHistoryResponse.from(
                    history,
                    actor != null ? actor.getNickname() : null,
                    target != null ? target.getNickname() : null
            );
        });
    }
}
