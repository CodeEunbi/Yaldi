package com.yaldi.domain.team.service;

import com.yaldi.domain.project.entity.Project;
import com.yaldi.domain.project.repository.ProjectRepository;
import com.yaldi.domain.team.dto.request.CreateTeamRequest;
import com.yaldi.domain.team.dto.request.InviteTeamMemberRequest;
import com.yaldi.domain.team.dto.request.UpdateTeamNameRequest;
import com.yaldi.domain.team.dto.response.*;
import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.entity.UserTeamRelation;
import com.yaldi.domain.team.repository.TeamRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.domain.team.validator.TeamValidator;
import com.yaldi.domain.user.entity.User;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserTeamRelationRepository userTeamRelationRepository;
    private final ProjectRepository projectRepository;
    private final TeamValidator teamValidator;
    private final TeamMembershipService membershipService;
    private final TeamOwnershipService ownershipService;
    private final TeamInvitationService invitationService;

    public List<TeamResponse> getUserTeams(Integer userKey) {
        List<UserTeamRelation> userTeamRelations = userTeamRelationRepository.findByUser_UserKey(
                userKey);

        List<Integer> teamKeys = userTeamRelations.stream()
                .map(relation -> relation.getTeam().getTeamKey())
                .collect(Collectors.toList());

        List<Team> teams = teamRepository.findActiveTeamsByIds(teamKeys);

        return teams.stream()
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    public List<TeamResponse> getOwnedTeams(Integer userKey) {
        List<Team> ownedTeams = teamRepository.findByOwnerUserKey(userKey);
        return ownedTeams.stream()
                .map(TeamResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamResponse createTeam(Integer userKey, CreateTeamRequest request) {
        // 전체 서비스에서 팀명 중복 확인
        if (teamRepository.existsByName(request.name())) {
            throw new GeneralException(ErrorStatus.TEAM_DUPLICATE_NAME);
        }

        User owner = teamValidator.getUser(userKey);
        Team team = Team.builder()
                .owner(owner)
                .name(request.name())
                .build();

        Team savedTeam = teamRepository.save(team);

        // 팀 생성자는 바로 팀 멤버로 추가
        UserTeamRelation relation = UserTeamRelation.builder()
                .user(owner)
                .team(savedTeam)
                .build();

        userTeamRelationRepository.save(relation);
        log.info("팀생성:: 팀키={}, 팀명={}, 오너={}", savedTeam.getTeamKey(), savedTeam.getName(),
                savedTeam.getOwner().getUserKey());
        return TeamResponse.from(savedTeam);
    }

    public TeamResponse getTeamDetail(Integer userKey, Integer teamKey) {
        Team team = teamValidator.getTeamWithMembershipCheck(teamKey, userKey);
        return TeamResponse.from(team);
    }

    @Transactional
    public TeamResponse updateTeamName(Integer userKey, Integer teamKey,
            UpdateTeamNameRequest request) {
        Team team = teamValidator.getTeamWithOwnerCheck(teamKey, userKey);

        if (!team.getName().equals(request.name())) {
            if (teamRepository.existsByName(request.name())) {
                throw new GeneralException(ErrorStatus.TEAM_DUPLICATE_NAME);
            }
        }
        team.updateName(request.name());
        return TeamResponse.from(team);
    }

    @Transactional
    public void deleteTeam(Integer userKey, Integer teamKey) {
        Team team = teamValidator.getTeamWithOwnerCheck(teamKey, userKey);

        List<Project> projects = projectRepository.findByTeamKey(teamKey);
        projects.forEach(project -> {
            project.softDelete();
            projectRepository.save(project);  // 명시적 save
        });

        userTeamRelationRepository.deleteByTeam_TeamKey(teamKey);

        team.softDelete();
        teamRepository.save(team);

        log.info("팀 삭제 완료 :: 팀키={}, 팀명={}, 삭제된 프로젝트 수={}", team.getTeamKey(), team.getName(),
                projects.size());
    }

    public List<TeamMemberResponse> getTeamMembers(Integer userKey, Integer teamKey) {
        Team team = teamValidator.getTeamWithMembershipCheck(teamKey, userKey);

        List<UserTeamRelation> userTeamRelations = userTeamRelationRepository.findByTeam_TeamKeyWithUser(
                teamKey);

        return userTeamRelations.stream()
                .map(relation -> TeamMemberResponse.from(relation, team, relation.getUser()))
                .collect(Collectors.toList());
    }


    @Transactional
    public void expelTeamMember(Integer userKey, Integer teamKey, Integer targetUserKey) {
        membershipService.expelTeamMember(userKey, teamKey, targetUserKey);
    }

    @Transactional
    public void leaveTeam(Integer userKey, Integer teamKey) {
        membershipService.leaveTeam(userKey, teamKey);
    }

    @Transactional
    public TeamResponse transferOwnership(Integer userKey, Integer teamKey, Integer newOwnerUserKey) {
        return ownershipService.transferOwnership(userKey, teamKey, newOwnerUserKey);
    }

    @Transactional
    public TeamInvitationResponse inviteTeamMember(Integer userKey, Integer teamKey,
                                                   InviteTeamMemberRequest request) {
        return invitationService.inviteTeamMember(userKey, teamKey, request);
    }

    @Transactional
    public InvitationActionResponse acceptInvitation(Integer userKey, Long invitationKey) {
        return membershipService.acceptInvitation(userKey, invitationKey);
    }

    @Transactional
    public InvitationActionResponse rejectInvitation(Integer userKey, Long invitationKey) {
        return membershipService.rejectInvitation(userKey, invitationKey);
    }

    @Transactional
    public void cancelInvitation(Integer userKey, Long invitationKey) {
        invitationService.cancelInvitation(userKey, invitationKey);
    }

    public List<UserSearchResult> searchUsersForInvitation(Integer userKey, Integer teamKey,
                                                            String keyword) {
        return invitationService.searchUsersForInvitation(userKey, teamKey, keyword);
    }

    public PendingInvitationListResponse getPendingInvitations(Integer userKey, Integer teamKey) {
        return invitationService.getPendingInvitations(userKey, teamKey);
    }
}
