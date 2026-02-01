package com.yaldi.global.asyncjob.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ValidateImportRequest {
    private String requestKey;
    private String userKey;
    private String projectKey;
    private String sqlContent;
    private String timestamp;
}


