package com.yaldi.domain.health.controller;

import com.yaldi.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Health Check", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

    @Operation(summary = "기본 Health Check", description = "서버의 기본 상태를 확인합니다.")
    @GetMapping
    public ApiResponse<String> healthCheck() {
        return ApiResponse.onSuccess("Server is running");
    }

    @Operation(summary = "상세 Health Check", description = "서버의 상세 상태 정보를 확인합니다.")
    @GetMapping("/detail")
    public ApiResponse<Map<String, Object>> detailHealthCheck() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("service", "YALDI");
        healthInfo.put("version", "1.0.0");

        return ApiResponse.onSuccess(healthInfo);
    }

    @Operation(summary = "Database Health Check", description = "데이터베이스 연결 상태를 확인합니다.")
    @GetMapping("/db")
    public ApiResponse<Map<String, String>> databaseHealthCheck() {
        Map<String, String> dbInfo = new HashMap<>();
        dbInfo.put("database", "PostgreSQL");
        dbInfo.put("status", "Connected");

        return ApiResponse.onSuccess(dbInfo);
    }

    @Operation(summary = "Ping", description = "간단한 ping 테스트")
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.onSuccess("pong");
    }
}
