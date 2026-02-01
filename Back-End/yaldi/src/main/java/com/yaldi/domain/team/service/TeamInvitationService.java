package com.yaldi.domain.team.service;

import com.yaldi.domain.notification.entity.Notification;
import com.yaldi.domain.notification.service.NotificationService;
import com.yaldi.domain.team.dto.request.InviteTeamMemberRequest;
import com.yaldi.domain.team.dto.response.PendingInvitationListResponse;
import com.yaldi.domain.team.dto.response.TeamInvitationResponse;
import com.yaldi.domain.team.dto.response.UserSearchResult;
import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.entity.UserTeamActionType;
import com.yaldi.domain.team.entity.UserTeamHistory;
import com.yaldi.domain.team.repository.UserTeamHistoryRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.domain.team.validator.TeamValidator;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.mail.MailService;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamInvitationService {

    private final UserTeamHistoryRepository userTeamHistoryRepository;
    private final UserTeamRelationRepository userTeamRelationRepository;
    private final UserRepository userRepository;
    private final TeamValidator teamValidator;
    private final TeamHistoryRecorder historyRecorder;
    private final MailService mailService;
    private final NotificationService notificationService;

    @Transactional
    public TeamInvitationResponse inviteTeamMember(Integer userKey, Integer teamKey,
                                                   InviteTeamMemberRequest request) {
        Team team = teamValidator.getTeamWithOwnerCheck(teamKey, userKey);

        User invitedUser = teamValidator.getUser(request.targetUserKey());

        if (invitedUser.getUserKey().equals(userKey)) {
            throw new GeneralException(ErrorStatus.TEAM_CANNOT_INVITE_SELF);
        }

        boolean isAlreadyMember = userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(
                invitedUser.getUserKey(), teamKey);
        if (isAlreadyMember) {
            throw new GeneralException(ErrorStatus.TEAM_DUPLICATE_MEMBER);
        }

        List<UserTeamHistory> histories = userTeamHistoryRepository
                .findByTeam_TeamKeyAndEmailOrderByCreatedAtAsc(
                        teamKey,
                        invitedUser.getEmail()
                );

        if (!histories.isEmpty()) {
            UserTeamHistory latest = histories.get(histories.size() - 1);

            // 최신 상태가 INVITE_SENT이고 만료되지 않았다면 → 재초대 불가
            if (latest.getActionType() == UserTeamActionType.INVITE_SENT && !latest.isExpired()) {
                throw new GeneralException(ErrorStatus.TEAM_INVITATION_ALREADY_EXISTS);
            }
        }

        User inviter = teamValidator.getUser(userKey);

        UserTeamHistory savedInvitation = historyRecorder.recordInvitationSent(
                team, inviter, invitedUser, invitedUser.getEmail());

        notificationService.notifyUser(
                request.targetUserKey(),
                UserTeamActionType.INVITE_SENT.getValue(),
                team.getName(),
                savedInvitation.getUserTeamHistoryKey()
        );

        mailService.sendTeamInvitation(invitedUser.getEmail(), team.getName(),
                inviter.getNickname());

        return TeamInvitationResponse.from(savedInvitation, team.getName(), inviter.getNickname());
    }

    @Transactional
    public void cancelInvitation(Integer userKey, Long invitationKey) {
        UserTeamHistory invitation = userTeamHistoryRepository.findById(invitationKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_INVITATION_NOT_FOUND));

        Team team = teamValidator.getTeamWithOwnerCheck(invitation.getTeam().getTeamKey(), userKey);

        // 이미 처리된 초대인지 확인
        if (invitation.getActionType() != UserTeamActionType.INVITE_SENT) {
            throw new GeneralException(ErrorStatus.TEAM_INVITATION_ALREADY_PROCESSED);
        }

        User actor = teamValidator.getUser(userKey);

        historyRecorder.recordInvitationCanceled(
                invitation.getTeam(), actor, invitation.getTarget(), invitation.getEmail());

        Notification notification = notificationService.getNotification(
                UserTeamActionType.INVITE_SENT.getValue(),
                invitation.getTarget().getUserKey(),
                invitationKey);
        notification.markAsRead();
        notification.changeType(UserTeamActionType.INVITE_CANCELED.getValue());

        log.info("팀 초대 취소 완료 :: 팀키={}, 취소한 유저={}", invitation.getTeam().getTeamKey(), userKey);
    }

    public List<UserSearchResult> searchUsersForInvitation(Integer userKey, Integer teamKey,
                                                            String keyword) {
        Team team = teamValidator.getTeamWithOwnerCheck(teamKey, userKey);

        // 키워드로 사용자 검색
        List<User> users = userRepository.searchActiveUsersByKeyword(keyword);

        // 팀 멤버 userKey 목록 조회 (N+1 방지 - 프로젝션 쿼리)
        Set<Integer> memberUserKeys = userTeamRelationRepository.findUserKeysByTeam_TeamKey(teamKey);

        // 대기 중인 초대 이메일 추출 (각 사용자별 가장 최신 상태 확인)
        Set<String> invitedEmails = users.stream()
                .map(User::getEmail)
                .filter(email -> {
                    // 각 이메일별 가장 최신 히스토리 조회
                    Optional<UserTeamHistory> latestHistory =
                            userTeamHistoryRepository.findFirstByTeam_TeamKeyAndEmailOrderByCreatedAtDesc(
                                    teamKey, email);

                    // 가장 최신 상태가 INVITE_SENT이면서 만료되지 않은 경우만 true
                    return latestHistory.isPresent()
                            && latestHistory.get().getActionType() == UserTeamActionType.INVITE_SENT
                            && !latestHistory.get().isExpired();
                })
                .collect(Collectors.toSet());

        // 각 사용자에 대해 상태 결정
        return users.stream()
                .map(user -> {
                    UserSearchResult.InviteStatus status;
                    if (memberUserKeys.contains(user.getUserKey())) {
                        status = UserSearchResult.InviteStatus.ALREADY_MEMBER;
                    } else if (invitedEmails.contains(user.getEmail())) {
                        status = UserSearchResult.InviteStatus.ALREADY_INVITED;
                    } else {
                        status = UserSearchResult.InviteStatus.INVITABLE;
                    }
                    return new UserSearchResult(user.getUserKey(), user.getNickname(),
                            user.getEmail(), status);
                })
                .collect(Collectors.toList());
    }

    public PendingInvitationListResponse getPendingInvitations(Integer userKey, Integer teamKey) {
        Team team = teamValidator.getTeamWithOwnerCheck(teamKey, userKey);

        // INVITE_SENT 상태이면서 만료되지 않은 초대 목록 조회 (actor, target fetch join으로 N+1 방지)
        List<UserTeamHistory> sentInvitations = userTeamHistoryRepository
                .findByTeam_TeamKeyAndActionTypeWithUsers(teamKey, UserTeamActionType.INVITE_SENT)
                .stream()
                .filter(h -> !h.isExpired())
                .collect(Collectors.toList());

        // 실제로 대기 중인 초대만 필터링 (이메일별 최신 이력이 INVITE_SENT인 경우만)
        List<UserTeamHistory> pendingInvitations = sentInvitations.stream()
                .filter(invitation -> {
                    // 해당 이메일의 최신 이력 조회
                    Optional<UserTeamHistory> latestHistory = userTeamHistoryRepository
                            .findFirstByTeam_TeamKeyAndEmailOrderByCreatedAtDesc(
                                    teamKey,
                                    invitation.getEmail()
                            );

                    // 최신 이력이 없거나, 최신 이력이 현재 초대가 아니면 제외
                    if (latestHistory.isEmpty()) {
                        return false;
                    }

                    UserTeamHistory latest = latestHistory.get();

                    // 최신 이력이 현재 조회된 INVITE_SENT와 동일한지 확인 (같은 레코드인지)
                    // 그리고 최신 이력의 액션 타입이 INVITE_SENT인지 확인
                    return latest.getUserTeamHistoryKey().equals(invitation.getUserTeamHistoryKey())
                            && latest.getActionType() == UserTeamActionType.INVITE_SENT;
                })
                .collect(Collectors.toList());

        // TeamInvitationResponse로 변환
        List<TeamInvitationResponse> invitationResponses = pendingInvitations.stream()
                .map(invitation -> TeamInvitationResponse.from(
                        invitation,
                        team.getName(),
                        invitation.getActor().getNickname()
                ))
                .collect(Collectors.toList());

        log.info("대기 중인 초대 목록 조회 완료 :: 팀키={}, 초대 수={}", teamKey, invitationResponses.size());

        return PendingInvitationListResponse.from(invitationResponses);
    }
}
