package com.yaldi.domain.team.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserTeamActionType {
    INVITE_SENT("INVITE_SENT"),
    INVITE_ACCEPTED("INVITE_ACCEPTED"),
    INVITE_REJECTED("INVITE_REJECTED"),
    INVITE_CANCELED("INVITE_CANCELED"),
    INVITE_EXPIRED("INVITE_EXPIRED"),
    MEMBER_EXITED("MEMBER_EXITED"),
    MEMBER_EXPULSION("MEMBER_EXPULSION"),
    MEMBER_WITHDRAWAL("MEMBER_WITHDRAWAL"),
    MEMBER_REJOIN("MEMBER_REJOIN"),
    OWNER_CHANGED("OWNER_CHANGED"),
    BE_TEAM_OWNER("BE_TEAM_OWNER"),
    REMOVED_FROM_TEAM("REMOVED_FROM_TEAM"),
    ADDED_TO_TEAM("ADDED_TO_TEAM");

    private final String value;
}
