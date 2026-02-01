package com.yaldi.domain.notification.sse;

import com.yaldi.infra.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationSseController {

    private final NotificationSseEmitterManager notificationSseEmitterManager;

    /**
     * 사용자 SSE 연결
     */
    @GetMapping("/stream")
    public SseEmitter connect() {
        return notificationSseEmitterManager.createEmitter(SecurityUtil.getCurrentUserKey());
    }
}
