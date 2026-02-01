package com.yaldi.domain.viewer.controller;

import com.yaldi.domain.erd.dto.response.ErdResponse;
import com.yaldi.domain.erd.service.ErdService;
import com.yaldi.domain.viewer.dto.ViewerLinkInfo;
import com.yaldi.domain.viewer.dto.response.ViewerLinkResponse;
import com.yaldi.domain.viewer.dto.response.ViewerLinkValidationResponse;
import com.yaldi.domain.viewer.service.ViewerLinkService;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Viewer", description = "뷰어링크 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/viewer")
@RequiredArgsConstructor
@Validated
public class ViewerController {

    private final ViewerLinkService viewerLinkService;
    private final ErdService erdService;

    @Operation(summary = "뷰어링크 생성/조회", description = "프로젝트 멤버만 뷰어링크를 생성하거나 기존 링크를 조회할 수 있습니다. (3일 유효)")
    @PostMapping("/projects/{projectKey}/link")
    public ApiResponse<ViewerLinkResponse> createOrGetViewerLink(
            @Parameter(description = "프로젝트 ID", required = true)
            @PathVariable @Min(value = 1, message = "프로젝트 ID는 1 이상이어야 합니다") Long projectKey
    ) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        log.info("뷰어링크 생성/조회 요청 - UserKey: {}, ProjectKey: {}", userKey, projectKey);

        // 프로젝트 멤버 권한 확인
        if (!viewerLinkService.isProjectMember(projectKey, userKey)) {
            log.warn("프로젝트 멤버가 아닌 사용자의 뷰어링크 생성 시도 - UserKey: {}, ProjectKey: {}", userKey, projectKey);
            throw new GeneralException(ErrorStatus.PROJECT_FORBIDDEN);
        }

        ViewerLinkResponse response = viewerLinkService.getOrCreateViewerLink(projectKey);
        return ApiResponse.onSuccess(response);
    }

    @Operation( summary = "뷰어링크 검증", description = "뷰어링크의 유효성을 검증합니다 " )
    @GetMapping("/{linkId}/validate")
    public ApiResponse<ViewerLinkValidationResponse> validateViewerLink(
            @Parameter(description = "뷰어링크 ID", required = true)
            @PathVariable String linkId
    ) {
        ViewerLinkInfo linkInfo = viewerLinkService.validateAndGetLinkInfo(linkId);

        boolean isAuthenticated = SecurityUtil.isAuthenticated();

        ViewerLinkValidationResponse response;

        if (isAuthenticated) {
            Integer userKey = SecurityUtil.getCurrentUserKey();
            boolean isMember = viewerLinkService.isProjectMember(linkInfo.projectKey(), userKey);

            if (isMember) {
                // 프로젝트 멤버 : 워크스페이스로 리다이렉트
                String workspaceUrl = "/project/" + linkInfo.projectKey() +"/workspace";
                response = ViewerLinkValidationResponse.forMember(linkInfo.projectKey(), workspaceUrl);
                log.info("프로젝트 멤버의 뷰어링크 접속 - UserKey: {}, ProjectKey: {}, Redirect: {}", userKey, linkInfo.projectKey(), workspaceUrl);
            } else {
                // 인증됐지만 멤버 아님 : 뷰어로 진행
                String viewerUrl = "/viewer/" + linkId;
                response = ViewerLinkValidationResponse.forViewer(linkInfo.projectKey(), viewerUrl);
                log.info("비멤버 인증 사용자의 뷰어링크 접속 - UserKey: {}, ProjectKey: {}", userKey, linkInfo.projectKey());
            }
        } else {
            // 미인증 : 뷰어로 진행
            String viewerUrl = "/viewer/" + linkId;
            response = ViewerLinkValidationResponse.forViewer(linkInfo.projectKey(), viewerUrl);
            log.info("미인증 사용자의 뷰어링크 접속 - ProjectKey: {}", linkInfo.projectKey());
        }
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "프로젝트 ERD 조회", description = "프로젝트의 전체 ERD 데이터를 조회합니다.")
    @GetMapping("/projects/{projectKey}")
    public ApiResponse<ErdResponse> getProjectErd(@PathVariable Long projectKey) {
        ErdResponse erd = erdService.getErdByProjectKey(projectKey);
        return ApiResponse.onSuccess(erd);
    }
}
