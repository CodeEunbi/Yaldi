package com.yaldi.domain.erd.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SQL Dialect 타입
 */
@Getter
@RequiredArgsConstructor
public enum SqlDialect {
    POSTGRESQL("PostgreSQL"),
    MYSQL("MySQL");

    private final String value;
}
