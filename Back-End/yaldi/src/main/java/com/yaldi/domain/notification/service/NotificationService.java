package com.yaldi.domain.notification.service;

import com.yaldi.domain.notification.entity.NotificationConverter;
import com.yaldi.domain.notification.dto.response.NotificationResponse;
import com.yaldi.domain.notification.entity.Notification;
import com.yaldi.domain.notification.repository.NotificationRepository;
import com.yaldi.domain.notification.sse.NotificationSseEmitterManager;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;

import com.yaldi.domain.team.entity.UserTeamActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseEmitterManager notificationSseEmitterManager;

    /**
     * 알림 생성 및 SSE 푸시
     */
    @Transactional
    public void notifyUser(Integer userKey, String type, String content, Long target) {
        Notification entity = Notification.builder()
                .userKey(userKey)
                .type(type)
                .target(target)
                .content(content)
                .build();

        notificationRepository.save(entity);
        notificationSseEmitterManager.sendToUser(userKey, entity);
    }

    @Transactional
    public Page<NotificationResponse> getAllNotifications(Integer userKey, Pageable pageable) {
        Page<Notification> notificationsPage =
                notificationRepository.findByUserKeyOrderByCreatedAtDesc(userKey, pageable);

        Page<NotificationResponse> response = notificationsPage.map(NotificationConverter::toResponse);

        for (Notification notification : notificationsPage) {
            if(notification.getType().equals(UserTeamActionType.INVITE_SENT.getValue())) continue;
            notification.markAsRead();
        }
        return response;
    }


    @Transactional
    public Notification getNotification(String type, Integer userKey, Long target){
        return notificationRepository.findByTypeAndUserKeyAndTarget(type,
                userKey, target).orElseThrow(() -> new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND));
    }
}
