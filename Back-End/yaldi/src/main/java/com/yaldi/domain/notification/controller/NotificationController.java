package com.yaldi.domain.notification.controller;

import com.yaldi.domain.notification.service.NotificationService;
import com.yaldi.domain.project.entity.ProjectMemberActionType;
import com.yaldi.domain.team.entity.UserTeamActionType;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.PageResponse;
import com.yaldi.infra.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.shaded.com.google.protobuf.Api;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     */
    @GetMapping
    public ApiResponse<?> getAll(Pageable pageable) {
        pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize() > 0 ? pageable.getPageSize() : 10,
                Sort.by(
                        Sort.Order.desc("createdAt")
                )
        );
        Integer userKey = SecurityUtil.getCurrentUserKey();
        return ApiResponse.onSuccess(PageResponse.of(
                notificationService.getAllNotifications(userKey, pageable)));
    }

    @GetMapping("/test")
    public void test() {
        notificationService.notifyUser(SecurityUtil.getCurrentUserKey(),
                UserTeamActionType.INVITE_SENT.getValue(), "name", null);
    }

    @GetMapping("/test2")
    public ApiResponse<?> test2() {
        return ApiResponse.onSuccess(
                notificationService.getNotification(UserTeamActionType.INVITE_SENT.getValue(), 1,
                        21L));
    }
}
