package com.yaldi.domain.version.dto.response;

import com.yaldi.domain.version.entity.DesignVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;


@Schema(description = "버전 검증 결과")
public record VersionVerificationResult(

        @Schema(description = "검증 성공 여부", example = "true")
        Boolean isValid,

        @Schema(description = "검증 상태", example = "SUCCESS")
        DesignVerificationStatus status,

        @Schema(description = "에러 목록")
        List<String> errors,

        @Schema(description = "경고 목록")
        List<String> warnings,

        @Schema(description = "검증 메시지", example = "스키마 검증이 성공적으로 완료되었습니다.")
        String message,

        @Schema(description = "검증 실패 시 LLM이 생성한 수정 조언")
        List<String> suggestions
) {
}
