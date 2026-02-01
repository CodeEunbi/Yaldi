package com.yaldi.domain.erd.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 참조 무결성 액션 타입
 *
 * DB ENUM Type: referential_action_type (00_init.sql에 정의됨)
 */
@Getter
@RequiredArgsConstructor
public enum ReferentialActionType {
    CASCADE("CASCADE"),
    SET_NULL("SET_NULL"),
    SET_DEFAULT("SET_DEFAULT"),
    RESTRICT("RESTRICT"),
    NO_ACTION("NO_ACTION");

    private final String value;
}
