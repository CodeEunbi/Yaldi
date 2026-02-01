package com.yaldi.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 멤버의 역할
 *
 * DB ENUM Type: project_member_role_type (00_init.sql에 정의됨)
 */
@Getter
@RequiredArgsConstructor
public enum ProjectMemberRole {
    OWNER("OWNER"),
    EDITOR("EDITOR"),
    ADMIN("ADMIN");

    private final String value;
}
