package com.yaldi.global.asyncjob.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ImportSqlRequest {
    private String sqlContent;
}
