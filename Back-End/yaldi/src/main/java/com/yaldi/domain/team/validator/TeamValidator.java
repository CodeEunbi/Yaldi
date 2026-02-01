package com.yaldi.domain.team.validator;

import com.yaldi.domain.team.entity.Team;
import com.yaldi.domain.team.repository.TeamRepository;
import com.yaldi.domain.team.repository.UserTeamRelationRepository;
import com.yaldi.domain.user.entity.User;
import com.yaldi.domain.user.repository.UserRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamValidator {

    private final TeamRepository teamRepository;
    private final UserTeamRelationRepository userTeamRelationRepository;
    private final UserRepository userRepository;

    public Team getActiveTeam(Integer teamKey) {
        return teamRepository.findActiveTeamById(teamKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.TEAM_NOT_FOUND));
    }

    public User getUser(Integer userKey) {
        return userRepository.findById(userKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    public void validateTeamMembership(Integer userKey, Integer teamKey) {
        boolean isMember = userTeamRelationRepository.existsByUser_UserKeyAndTeam_TeamKey(userKey, teamKey);
        if (!isMember) {
            throw new GeneralException(ErrorStatus.TEAM_FORBIDDEN);
        }
    }

    public void validateTeamOwner(Team team, Integer userKey) {
        if (!team.getOwner().getUserKey().equals(userKey)) {
            throw new GeneralException(ErrorStatus.TEAM_OWNER_ONLY);
        }
    }

    public Team getTeamWithMembershipCheck(Integer teamKey, Integer userKey) {
        Team team = getActiveTeam(teamKey);
        validateTeamMembership(userKey, teamKey);
        return team;
    }

    public Team getTeamWithOwnerCheck(Integer teamKey, Integer userKey) {
        Team team = getActiveTeam(teamKey);
        validateTeamMembership(userKey, teamKey);
        validateTeamOwner(team, userKey);
        return team;
    }
}
