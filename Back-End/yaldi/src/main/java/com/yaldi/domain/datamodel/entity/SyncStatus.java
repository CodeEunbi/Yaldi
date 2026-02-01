package com.yaldi.domain.datamodel.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 데이터 모델 동기화 상태
 */
@Getter
@RequiredArgsConstructor
public enum SyncStatus {
    IN_SYNC("IN_SYNC", "동기화됨", true),
    OUT_OF_SYNC("OUT_OF_SYNC", "변경사항 있음", true),
    INVALID("INVALID", "무효", false);

    private final String value;
    private final String description;
    private final boolean canRefresh;  // Refresh 가능 여부
}
