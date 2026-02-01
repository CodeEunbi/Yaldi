package com.yaldi.domain.version.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yaldi.global.asyncjob.enums.AsyncJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mock 데이터 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MockDataResponse(

        @Schema(description = "Mock Data Key", example = "123")
        Long mockDataKey,

        @Schema(description = "작업 상태", example = "PENDING")
        AsyncJobStatus status,

        @Schema(description = "버전 Key", example = "10")
        Long versionKey,

        @Schema(description = "생성할 행 수", example = "100")
        Integer rowCount,

        @Schema(description = "파일명 (완료 시)", example = "mock_data_v1_0_0_1234567890.sql")
        String fileName,

        @Schema(description = "다운로드 URL (완료 시, Presigned URL)")
        String downloadUrl
) {
}
