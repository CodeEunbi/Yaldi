package com.yaldi.global.response.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus {

    // Common
    OK(HttpStatus.OK, "COMMON200", "성공입니다."),
    CREATED(HttpStatus.CREATED, "COMMON201", "생성되었습니다."),
    ACCEPTED(HttpStatus.ACCEPTED, "COMMON202", "요청이 수락되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "COMMON204", "삭제되었습니다."),
    RESET_CONTENT(HttpStatus.RESET_CONTENT, "COMMON205", "요청이 성공했으며 입력 폼을 초기화하세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
