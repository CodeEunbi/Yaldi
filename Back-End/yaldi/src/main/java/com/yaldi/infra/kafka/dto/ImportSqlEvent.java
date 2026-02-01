package com.yaldi.infra.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImportSqlEvent {
    private String jobId;
    private Long projectKey;
    private Integer userKey;
    private String sqlContent;
}