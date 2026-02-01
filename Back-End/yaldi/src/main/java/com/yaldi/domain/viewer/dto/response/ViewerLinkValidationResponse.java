package com.yaldi.domain.viewer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "뷰어링크 검증 응답")
public record ViewerLinkValidationResponse(
        @Schema(description = "프로젝트 멤버 여부", example = "true")
        boolean isMember,

        @Schema(description = "프로젝트 키", example = "123")
        Long projectKey,

        @Schema(description = "워크스페이스 리다이렉트 URL (멤버인 경우)", example = "/project/:projectKey/workspace")
        String redirectUrl,

        @Schema(description = "뷰어 URL (멤버가 아닌 경우)", example = "/viewer/abc-123")
        String viewerUrl
) {
    // 프로젝트 멤버용
    public static ViewerLinkValidationResponse forMember(Long projectKey, String workspaceUrl) {
        return new ViewerLinkValidationResponse(
                true,
                projectKey,
                workspaceUrl,
                null
        );
    }

    // 뷰어용 응답
    public static ViewerLinkValidationResponse forViewer(Long projectKey, String viewerUrl) {
        return new ViewerLinkValidationResponse(
                false,
                projectKey,
                null,
                viewerUrl
        );
    }
}
