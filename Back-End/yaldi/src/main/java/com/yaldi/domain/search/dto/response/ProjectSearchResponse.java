package com.yaldi.domain.search.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 버전 검색 결과를 프로젝트별로 그룹화하여 반환
 * 검색 결과 화면에서 프로젝트 썸네일로 표시됨
 */

@Schema(description = "그룹핑한 프로젝트 정보")
public record ProjectSearchResponse(

        @Schema(description = "프로젝트 ID", example = "1")
        Long projectKey,

        @Schema(description = "프로젝트 이름", example = "중고거래")
        String projectName,

        @Schema(description = "프로젝트 설명", example = "중고거래 erd 입니다")
        String projectDescription,

        @Schema(description = "프로젝트 썸네일 이미지")
        String imageUrl
) {
}
