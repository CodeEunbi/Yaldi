package com.yaldi.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectMemberActionType {
    ADD("ADD"),
    EXPULSION("EXPULSION"),
    EXIT("EXIT"),
    WITHDRAWAL("WITHDRAWAL"),
    REJOIN("REJOIN"),
    ROLE_CHANGED("ROLE_CHANGED");

    private final String value;
}
