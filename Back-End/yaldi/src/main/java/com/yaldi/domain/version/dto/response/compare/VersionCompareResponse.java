package com.yaldi.domain.version.dto.response.compare;

import com.yaldi.domain.version.dto.response.VersionResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "버전 비교 응답")
public record VersionCompareResponse(
        @Schema(description = "이전 버전 (없으면 null)")
        VersionResponse previousVersion,

        @Schema(description = "현재 버전")
        VersionResponse currentVersion,

        @Schema(description = "스키마 차이 정보 (이전 버전이 있을 때만)")
        SchemaDiff schemaDiff
) {
}
