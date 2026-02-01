package com.yaldi.domain.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 프로젝트 수정 요청 DTO
 */
@Schema(description = "프로젝트 수정 요청")
public record UpdateProjectRequest(
    @Schema(description = "프로젝트 이름", example = "이커머스 프로젝트 v2")
    @Size(max = 25, message = "프로젝트 이름은 최대 25자까지 입력 가능합니다")
    String name,

    @Schema(description = "프로젝트 설명", example = "온라인 쇼핑몰 데이터베이스 설계 - 리뉴얼")
    @Size(max = 1000, message = "프로젝트 설명은 최대 1000자까지 입력 가능합니다")
    String description,

    @Schema(description = "프로젝트 이미지 URL")
    @Size(max = 10000, message = "이미지 URL은 최대 10000자까지 입력 가능합니다")
    String imageUrl
) {
}
