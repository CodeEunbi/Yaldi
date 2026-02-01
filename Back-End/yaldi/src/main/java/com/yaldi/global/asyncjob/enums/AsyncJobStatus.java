package com.yaldi.global.asyncjob.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AsyncJobStatus {
    PENDING("PENDING", "대기 중"),
    PROCESSING("PROCESSING", "처리 중"),
    COMPLETED("COMPLETED", "완료"),
    FAILED("FAILED", "실패");

    private final String value;
    private final String description;
}
