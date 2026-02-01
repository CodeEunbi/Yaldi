package com.yaldi.domain.team.service;

import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.entity.UserTeamActionType;
import com.yaldi.domain.team.entity.UserTeamHistory;
import com.yaldi.domain.team.repository.UserTeamHistoryRepository;
import com.yaldi.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamHistoryRecorder {

    private final UserTeamHistoryRepository userTeamHistoryRepository;

    public UserTeamHistory recordInvitationSent(Team team, User inviter, User target, String email) {
        UserTeamHistory invitation = UserTeamHistory.builder()
                .team(team)
                .actor(inviter)
                .target(target)
                .email(email)
                .actionType(UserTeamActionType.INVITE_SENT)
                .reason("팀 초대 전송")
                .build();
        return userTeamHistoryRepository.save(invitation);
    }

    public void recordInvitationAccepted(Team team, User user, String email) {
        UserTeamHistory acceptedHistory = UserTeamHistory.builder()
                .team(team)
                .actor(user)
                .target(user)
                .email(email)
                .actionType(UserTeamActionType.INVITE_ACCEPTED)
                .reason("팀 초대 수락")
                .build();
        userTeamHistoryRepository.save(acceptedHistory);
    }

    public void recordInvitationRejected(Team team, User user, String email) {
        UserTeamHistory rejectedHistory = UserTeamHistory.builder()
                .team(team)
                .actor(user)
                .target(user)
                .email(email)
                .actionType(UserTeamActionType.INVITE_REJECTED)
                .reason("팀 초대 거절")
                .build();
        userTeamHistoryRepository.save(rejectedHistory);
    }

    public void recordInvitationCanceled(Team team, User actor, User target, String email) {
        UserTeamHistory canceledHistory = UserTeamHistory.builder()
                .team(team)
                .actor(actor)
                .target(target)
                .email(email)
                .actionType(UserTeamActionType.INVITE_CANCELED)
                .reason("팀 초대 취소")
                .build();
        userTeamHistoryRepository.save(canceledHistory);
    }

    public void recordMemberExpulsion(Team team, User actor, User target) {
        UserTeamHistory expulsionHistory = UserTeamHistory.builder()
                .team(team)
                .actor(actor)
                .target(target)
                .email(target.getEmail())
                .actionType(UserTeamActionType.MEMBER_EXPULSION)
                .reason("팀 오너에 의한 방출")
                .build();
        userTeamHistoryRepository.save(expulsionHistory);
    }

    public void recordMemberExit(Team team, User user) {
        UserTeamHistory exitHistory = UserTeamHistory.builder()
                .team(team)
                .actor(user)
                .target(user)
                .email(user.getEmail())
                .actionType(UserTeamActionType.MEMBER_EXITED)
                .reason("팀 나가기")
                .build();
        userTeamHistoryRepository.save(exitHistory);
    }

    public void recordOwnerChange(Team team, User currentOwner, User newOwner) {
        UserTeamHistory ownerChangeHistory = UserTeamHistory.builder()
                .team(team)
                .actor(currentOwner)
                .target(newOwner)
                .email(newOwner.getEmail())
                .actionType(UserTeamActionType.OWNER_CHANGED)
                .reason("팀 오너 양도")
                .build();
        userTeamHistoryRepository.save(ownerChangeHistory);
    }
}
