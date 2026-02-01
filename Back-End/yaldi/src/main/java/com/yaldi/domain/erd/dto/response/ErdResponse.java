package com.yaldi.domain.erd.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 프로젝트 전체 ERD 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdResponse {

    private Long projectKey;
    private List<ErdTableResponse> tables;
    private List<ErdColumnResponse> columns;
    private List<ErdRelationResponse> relations;
}
