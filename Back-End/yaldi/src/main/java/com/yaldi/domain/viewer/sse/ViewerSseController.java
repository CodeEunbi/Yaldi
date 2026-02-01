package com.yaldi.domain.viewer.sse;

import com.yaldi.domain.viewer.dto.ViewerLinkInfo;
import com.yaldi.domain.viewer.service.ViewerLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Viewer SSE", description = "뷰어 SSE 스트리밍 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/viewer")
@RequiredArgsConstructor
public class ViewerSseController {

    private final ViewerLinkService viewerLinkService;
    private final ViewerSseEmitterManager viewerSseEmitterManager;

    @Operation(summary = "뷰어 SSE 스트림 연결", description = "뷰어링크를 통해 접속 시 실시간 ERD 업데이트를 수신하는 SSE 스트림을 시작합니다. ")
    @GetMapping(value = "/{linkId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @Parameter(description = "뷰어링크 ID", required = true)
            @PathVariable String linkId
    ) {
        log.info("뷰어 SSE 연결 요청 - LinkId: {}", linkId);

        ViewerLinkInfo linkInfo = viewerLinkService.validateAndGetLinkInfo(linkId);

        log.info("뷰어 SSE 연결 성공 - LinkId: {}, ProjectKey: {}", linkId, linkInfo.projectKey());

        // SSE Emitter 생성 및 반환
        return viewerSseEmitterManager.createEmitter(linkInfo.projectKey());
    }
}
