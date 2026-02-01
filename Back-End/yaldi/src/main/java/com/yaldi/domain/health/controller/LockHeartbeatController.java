package com.yaldi.domain.health.controller;

import com.yaldi.domain.health.service.LockHeartbeatService;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.util.LockUtil;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/heartbeat")
@RequiredArgsConstructor
public class LockHeartbeatController {
    private final LockUtil lockUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private final LockHeartbeatService lockHeartBeatService;

    @PostMapping("/{tableId}")
    public ApiResponse<?> heartbeat(@PathVariable Long tableId, @RequestParam String email) {
        lockHeartBeatService.handleHeartbeat(tableId, email);
        return ApiResponse.OK;
    }
}
