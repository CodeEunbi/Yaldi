package com.yaldi.domain.edithistory.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EditHistoryTargetType {
    TABLE("TABLE"),
    COLUMN("COLUMN"),
    RELATION("RELATION");

    private final String value;
}
