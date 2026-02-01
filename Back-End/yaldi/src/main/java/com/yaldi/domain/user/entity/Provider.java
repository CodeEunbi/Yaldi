package com.yaldi.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE("GOOGLE"),
    GITHUB("GITHUB"),
    SSAFY("SSAFY");

    private final String value;
}
