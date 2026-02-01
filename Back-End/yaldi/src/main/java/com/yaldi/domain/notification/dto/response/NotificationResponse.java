package com.yaldi.domain.notification.dto.response;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long notificationKey,
        String type,
        String content,

        Long target,
        OffsetDateTime createdAt,
        boolean isRead
) {}
