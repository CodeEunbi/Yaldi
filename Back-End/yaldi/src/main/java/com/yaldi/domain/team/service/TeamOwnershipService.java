package com.yaldi.domain.team.service;

import com.yaldi.domain.notification.service.NotificationService;
import com.yaldi.domain.team.dto.response.TeamResponse;
import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.entity.UserTeamActionType;
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
public class TeamOwnershipService {

    private final UserTeamRelationRepository userTeamRelationRepository;
    private final TeamValidator teamValidator;
    private final TeamHistoryRecorder historyRecorder;
    private final NotificationService notificationService;

    @Transactional
    public TeamResponse transferOwnership(Integer userKey, Integer teamKey, Integer newOwnerUserKey) {
        Team team = teamValidator.getTeamWithOwnerCheck(teamKey, userKey);

        if (userKey.equals(newOwnerUserKey)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }

        boolean isNewOwnerMember = userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(
                newOwnerUserKey, teamKey);
        if (!isNewOwnerMember) {
            throw new GeneralException(ErrorStatus.TEAM_MEMBER_NOT_FOUND);
        }

        User currentOwner = teamValidator.getUser(userKey);
        User newOwner = teamValidator.getUser(newOwnerUserKey);

        team.updateOwner(newOwner);

        historyRecorder.recordOwnerChange(team, currentOwner, newOwner);

        notificationService.notifyUser(
                newOwnerUserKey,
                UserTeamActionType.BE_TEAM_OWNER.getValue(),
                team.getName(),
                Long.valueOf(team.getTeamKey()));

        log.info("팀 오너 변경 완료 :: 팀키={}, 이전 오너={}, 새 오너={}", teamKey, userKey, newOwnerUserKey);

        return TeamResponse.from(team);
    }
}
