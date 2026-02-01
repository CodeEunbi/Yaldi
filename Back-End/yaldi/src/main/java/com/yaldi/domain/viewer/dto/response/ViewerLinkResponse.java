package com.yaldi.domain.viewer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "뷰어링크 응답")
public record ViewerLinkResponse(
        @Schema(description = "뷰어링크 ID", example = "abc-123-def-456")
        String linkId,

        @Schema(description = "뷰어 URL", example = "https://yaldi.com/viewer/abc-123-def-456")
        String viewerUrl,

        @Schema(description = "남은 유효시간 (초)", example = "259200")
        Long remainingTtlSeconds,

        @Schema(description = "만료 시각")
        OffsetDateTime expiresAt
) {
    public static ViewerLinkResponse of(String linkId, String baseUrl, Long ttlSeconds) {
        String viewerUrl = baseUrl + "/viewer/" + linkId;
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(ttlSeconds);

        return new ViewerLinkResponse(linkId, viewerUrl, ttlSeconds, expiresAt);
    }
}
