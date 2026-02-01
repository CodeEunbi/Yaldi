package com.yaldi.domain.version.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DesignVerificationStatus {
    QUEUED("QUEUED"),
    RUNNING("RUNNING"),
    SUCCESS("SUCCESS"),
    WARNING("WARNING"),
    FAILED("FAILED"),
    CANCELED("CANCELED");

    private final String value;
}
