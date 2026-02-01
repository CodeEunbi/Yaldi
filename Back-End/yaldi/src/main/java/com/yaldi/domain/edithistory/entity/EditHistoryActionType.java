package com.yaldi.domain.edithistory.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EditHistoryActionType {
    ADD("ADD"),
    UPDATE("UPDATE"),
    RENAME("RENAME"),
    DELETE("DELETE");

    private final String value;
}
