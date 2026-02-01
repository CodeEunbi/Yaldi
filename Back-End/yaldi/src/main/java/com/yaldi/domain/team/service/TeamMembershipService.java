package com.yaldi.domain.team.service;

import com.yaldi.domain.notification.entity.Notification;
import com.yaldi.domain.notification.service.NotificationService;
import com.yaldi.domain.team.dto.response.InvitationActionResponse;
import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.entity.UserTeamActionType;
import com.yaldi.domain.team.entity.UserTeamHistory;
import com.yaldi.domain.team.entity.UserTeamRelation;
import com.yaldi.domain.team.repository.UserTeamHistoryRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.domain.team.validator.TeamValidator;
import com.yaldi.domain.user.entity.User;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMembershipService {

    private final UserTeamRelationRepository userTeamRelationRepository;
    private final UserTeamHistoryRepository userTeamHistoryRepository;
    private final TeamValidator teamValidator;
    private final TeamHistoryRecorder historyRecorder;
    private final NotificationService notificationService;

    @Transactional
    public void expelTeamMember(Integer userKey, Integer teamKey, Integer targetUserKey) {
        Team team = teamValidator.getTeamWithOwnerCheck(teamKey, userKey);

        if (userKey.equals(targetUserKey)) {
            throw new GeneralException(ErrorStatus.TEAM_OWNER_CANNOT_LEAVE);
        }

        UserTeamRelation targetRelation = userTeamRelationRepository.findByUser_UserKeyAndTeam_TeamKey(
                        targetUserKey, teamKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_MEMBER_NOT_FOUND));

        userTeamRelationRepository.delete(targetRelation);

        User actor = teamValidator.getUser(userKey);
        User targetUser = teamValidator.getUser(targetUserKey);

        historyRecorder.recordMemberExpulsion(team, actor, targetUser);

        log.info("팀 멤버 방출 완료 :: 팀키={}, 방출된 유저={}, 방출한 유저={}", teamKey, targetUserKey, userKey);

        notificationService.notifyUser(
                targetUserKey,
                UserTeamActionType.REMOVED_FROM_TEAM.getValue(),
                team.getName(),
                null);
    }

    @Transactional
    public void leaveTeam(Integer userKey, Integer teamKey) {
        Team team = teamValidator.getActiveTeam(teamKey);

        UserTeamRelation userRelation = userTeamRelationRepository.findByUser_UserKeyAndTeam_TeamKey(
                        userKey, teamKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_MEMBER_NOT_FOUND));

        if (team.getOwner().getUserKey().equals(userKey)) {
            throw new GeneralException(ErrorStatus.TEAM_OWNER_CANNOT_LEAVE);
        }

        userTeamRelationRepository.delete(userRelation);

        User user = teamValidator.getUser(userKey);
        historyRecorder.recordMemberExit(team, user);

        log.info("팀 탈퇴 완료 :: 팀키={}, 탈퇴한 유저={}", teamKey, userKey);
    }

    @Transactional
    public InvitationActionResponse acceptInvitation(Integer userKey, Long invitationKey) {
        User user = teamValidator.getUser(userKey);

        UserTeamHistory invitation = userTeamHistoryRepository.findById(invitationKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_INVITATION_NOT_FOUND));

        // 초대받은 사람이 맞는지 확인
        if (!invitation.getEmail().equals(user.getEmail())) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }

        // 이미 처리된 초대인지 확인
        if (invitation.getActionType() != UserTeamActionType.INVITE_SENT) {
            throw new GeneralException(ErrorStatus.TEAM_INVITATION_ALREADY_PROCESSED);
        }

        Team team = invitation.getTeam();

        // 만료된 초대인지 확인
        if (invitation.isExpired()) {
            Notification notification = notificationService.getNotification(
                    UserTeamActionType.INVITE_SENT.getValue(), userKey, invitationKey);
            notification.markAsRead();
            notification.changeType(UserTeamActionType.INVITE_EXPIRED.getValue());
            return InvitationActionResponse.expired(invitationKey, team.getTeamKey());
        }

        // 취소된 초대인지 확인 (초대 이후에 취소 이력이 있는지)
        boolean wasCanceled = userTeamHistoryRepository
                .findByTeam_TeamKeyAndEmailAndActionType(team.getTeamKey(), invitation.getEmail(),
                        UserTeamActionType.INVITE_CANCELED)
                .stream()
                .anyMatch(h -> h.getCreatedAt().isAfter(invitation.getCreatedAt()));

        if (wasCanceled) {
            throw new GeneralException(ErrorStatus.TEAM_INVITATION_CANCELED);
        }

        // 이미 팀 멤버인지 확인
        boolean isAlreadyMember = userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(
                userKey, team.getTeamKey());
        if (isAlreadyMember) {
            throw new GeneralException(ErrorStatus.TEAM_DUPLICATE_MEMBER);
        }

        // 팀 멤버로 추가
        UserTeamRelation relation = UserTeamRelation.builder()
                .user(user)
                .team(team)
                .build();
        userTeamRelationRepository.save(relation);

        historyRecorder.recordInvitationAccepted(team, user, user.getEmail());

        Notification notification = notificationService.getNotification(
                UserTeamActionType.INVITE_SENT.getValue(), userKey, invitationKey);
        notification.markAsRead();
        notification.changeType(UserTeamActionType.INVITE_ACCEPTED.getValue());

        notificationService.notifyUser(
                userKey,
                UserTeamActionType.ADDED_TO_TEAM.getValue(),
                team.getName(),
                null
        );

        log.info("팀 초대 수락 완료 :: 팀키={}, 유저={}", team.getTeamKey(), userKey);

        return InvitationActionResponse.accepted(invitationKey, team.getTeamKey());
    }

    @Transactional
    public InvitationActionResponse rejectInvitation(Integer userKey, Long invitationKey) {
        User user = teamValidator.getUser(userKey);

        UserTeamHistory invitation = userTeamHistoryRepository.findById(invitationKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_INVITATION_NOT_FOUND));

        // 초대받은 사람이 맞는지 확인
        if (!invitation.getEmail().equals(user.getEmail())) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }

        // 이미 처리된 초대인지 확인
        if (invitation.getActionType() != UserTeamActionType.INVITE_SENT) {
            throw new GeneralException(ErrorStatus.TEAM_INVITATION_ALREADY_PROCESSED);
        }

        Team team = invitation.getTeam();

        // 만료된 초대인지 확인
        if (invitation.isExpired()) {
            Notification notification = notificationService.getNotification(
                    UserTeamActionType.INVITE_SENT.getValue(), userKey, invitationKey);
            notification.markAsRead();
            notification.changeType(UserTeamActionType.INVITE_EXPIRED.getValue());
            return InvitationActionResponse.expired(invitationKey, team.getTeamKey());
        }

        // 취소된 초대인지 확인 (초대 이후에 취소 이력이 있는지)
        boolean wasCanceled = userTeamHistoryRepository
                .findByTeam_TeamKeyAndEmailAndActionType(team.getTeamKey(), invitation.getEmail(),
                        UserTeamActionType.INVITE_CANCELED)
                .stream()
                .anyMatch(h -> h.getCreatedAt().isAfter(invitation.getCreatedAt()));

        if (wasCanceled) {
            throw new GeneralException(ErrorStatus.TEAM_INVITATION_CANCELED);
        }

        historyRecorder.recordInvitationRejected(team, user, user.getEmail());

        Notification notification = notificationService.getNotification(
                UserTeamActionType.INVITE_SENT.getValue(), userKey, invitationKey);
        notification.markAsRead();
        notification.changeType(UserTeamActionType.INVITE_REJECTED.getValue());

        log.info("팀 초대 거절 완료 :: 팀키={}, 유저={}", team.getTeamKey(), userKey);

        return InvitationActionResponse.rejected(invitationKey, team.getTeamKey());
    }
}
