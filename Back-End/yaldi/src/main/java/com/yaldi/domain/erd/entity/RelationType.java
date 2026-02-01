package com.yaldi.domain.erd.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ERD 관계 타입
 *
 * DB ENUM Type: relation_type (00_init.sql에 정의됨)
 */
@Getter
@RequiredArgsConstructor
public enum RelationType {
    OPTIONAL_ONE_TO_ONE("OPTIONAL_ONE_TO_ONE"),
    STRICT_ONE_TO_ONE("STRICT_ONE_TO_ONE"),
    OPTIONAL_ONE_TO_MANY("OPTIONAL_ONE_TO_MANY"),
    STRICT_ONE_TO_MANY("STRICT_ONE_TO_MANY");

    private final String value;
}
