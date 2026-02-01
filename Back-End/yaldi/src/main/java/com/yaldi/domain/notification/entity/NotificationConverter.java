package com.yaldi.domain.notification.entity;

import com.yaldi.domain.notification.dto.response.NotificationResponse;

import java.util.List;

public class NotificationConverter {

    public static NotificationResponse toResponse(Notification entity) {
        return new NotificationResponse(
                entity.getNotificationKey(),
                entity.getType(),
                entity.getContent(),
                entity.getTarget(),
                entity.getCreatedAt(),
                entity.getReadAt() != null
        );
    }

    public static List<NotificationResponse> toResponseList(List<Notification> entities) {
        return entities.stream()
                .map(NotificationConverter::toResponse)
                .toList();
    }
}
