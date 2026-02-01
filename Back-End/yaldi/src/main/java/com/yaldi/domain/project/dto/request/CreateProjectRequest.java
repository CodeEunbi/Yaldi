package com.yaldi.domain.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 프로젝트 생성 요청 DTO
 */
@Schema(description = "프로젝트 생성 요청")
public record CreateProjectRequest(
    @Schema(description = "팀 ID", example = "1")
    @NotNull(message = "팀 ID는 필수입니다")
    @Min(value = 1, message = "팀 ID는 1 이상이어야 합니다")
    Integer teamKey,

    @Schema(description = "프로젝트 이름", example = "이커머스 프로젝트")
    @NotBlank(message = "프로젝트 이름은 필수입니다")
    @Size(max = 25, message = "프로젝트 이름은 최대 25자까지 입력 가능합니다")
    String name,

    @Schema(description = "프로젝트 설명", example = "온라인 쇼핑몰 데이터베이스 설계")
    @Size(max = 1000, message = "프로젝트 설명은 최대 1000자까지 입력 가능합니다")
    String description,

    @Schema(description = "프로젝트 이미지 URL")
    @Size(max = 10000, message = "이미지 URL은 최대 10000자까지 입력 가능합니다")
    String imageUrl
) {
}
