package com.yaldi.domain.datamodel.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DataModelType {
    ENTITY("ENTITY"),
    DTO_REQUEST("DTO_REQUEST"),
    DTO_RESPONSE("DTO_RESPONSE");

    private final String value;
}
