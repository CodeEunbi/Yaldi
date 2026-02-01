package com.yaldi.domain.erd.controller;

import com.yaldi.domain.erd.dto.request.ErdColumnCreateRequest;
import com.yaldi.domain.erd.dto.request.ErdColumnUpdateRequest;
import com.yaldi.domain.erd.dto.request.ErdRelationCreateRequest;
import com.yaldi.domain.erd.dto.request.ErdRelationUpdateRequest;
import com.yaldi.domain.erd.dto.request.ErdTableCreateRequest;
import com.yaldi.domain.erd.dto.request.ErdTableUpdateRequest;
import com.yaldi.domain.erd.dto.response.ErdColumnResponse;
import com.yaldi.domain.erd.dto.response.ErdRelationResponse;
import com.yaldi.domain.erd.dto.response.ErdRelationWithFkResponse;
import com.yaldi.domain.erd.dto.response.ErdResponse;
import com.yaldi.domain.erd.dto.response.ErdTableResponse;
import com.yaldi.domain.erd.dto.websocket.event.ColumnDelEvent;
import com.yaldi.domain.erd.dto.websocket.event.ColumnNewEvent;
import com.yaldi.domain.erd.dto.websocket.event.ColumnUpdateEvent;
import com.yaldi.domain.erd.dto.websocket.event.RelationDelEvent;
import com.yaldi.domain.erd.dto.websocket.event.RelationNewEvent;
import com.yaldi.domain.erd.dto.websocket.event.RelationUpdateEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableColorEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableDelEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableLnameEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableMoveEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableNewEvent;
import com.yaldi.domain.erd.dto.websocket.event.TablePnameEvent;
import com.yaldi.domain.erd.entity.SqlDialect;
import com.yaldi.domain.erd.service.ErdColumnService;
import com.yaldi.domain.erd.service.ErdExportService;
import com.yaldi.domain.erd.service.ErdRelationService;
import com.yaldi.domain.erd.service.ErdService;
import com.yaldi.domain.erd.service.ErdTableService;
import com.yaldi.domain.project.service.ProjectAccessValidator;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.ApiResponse;
import com.yaldi.global.response.status.ErrorStatus;
import com.yaldi.infra.security.util.SecurityUtil;
import com.yaldi.infra.websocket.dto.ErdBroadcastEvent;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ERD 관리 API
 */
@Tag(name = "ERD", description = "ERD 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/erd")
@RequiredArgsConstructor
public class ErdController {

    private final ErdService erdService;
    private final ErdTableService erdTableService;
    private final ErdColumnService erdColumnService;
    private final ErdRelationService erdRelationService;
    private final ErdExportService erdExportService;
    private final ProjectAccessValidator projectAccessValidator;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 프로젝트의 전체 ERD 조회
     */
    @Operation(summary = "프로젝트 ERD 조회", description = "프로젝트의 전체 ERD 데이터를 조회합니다.")
    @GetMapping("/projects/{projectKey}")
    public ApiResponse<ErdResponse> getProjectErd(@PathVariable Long projectKey) {
        // 프로젝트 접근 권한 검증
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        ErdResponse erd = erdService.getErdByProjectKey(projectKey);
        return ApiResponse.onSuccess(erd);
    }


    /**
     * ERD SQL Export
     */
    @Operation(summary = "ERD SQL Export", description = "프로젝트의 ERD를 SQL DDL로 Export합니다.")
    @GetMapping("/projects/{projectKey}/export/sql")
    public ApiResponse<String> exportErdToSql(
            @PathVariable Long projectKey,
            @RequestParam(defaultValue = "POSTGRESQL") SqlDialect dialect
    ) {
        // 프로젝트 접근 권한 검증
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        String sqlDdl = erdExportService.exportToSql(projectKey, dialect);
        return ApiResponse.onSuccess(sqlDdl);
    }

    // ========== ERD Table API ==========

    /**
     * ERD 테이블 생성
     */
    @Operation(summary = "ERD 테이블 생성", description = "새로운 ERD 테이블을 생성합니다.")
    @PostMapping("/projects/{projectKey}/tables")
    public ApiResponse<ErdTableResponse> createTable(
            @PathVariable Long projectKey,
            @Valid @RequestBody ErdTableCreateRequest request) {
        // 프로젝트 접근 권한 검증
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        ErdTableResponse table = erdTableService.createTable(projectKey, request, userKey);

        // Kafka 브로드캐스트 (전체 데이터)
        broadcast(TableNewEvent.builder()
                .tableKey(table.getTableKey())
                .projectKey(table.getProjectKey())
                .logicalName(table.getLogicalName())
                .physicalName(table.getPhysicalName())
                .xPosition(table.getXPosition())
                .yPosition(table.getYPosition())
                .colorHex(table.getColorHex())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build());

        return ApiResponse.onSuccess(table);
    }

    /**
     * ERD 테이블 수정
     */
    @Operation(summary = "ERD 테이블 수정", description = "ERD 테이블을 수정합니다.")
    @PatchMapping("/tables/{tableKey}")
    public ApiResponse<ErdTableResponse> updateTable(
            @PathVariable Long tableKey,
            @Valid @RequestBody ErdTableUpdateRequest request) {
        // 프로젝트 접근 권한 검증 (tableKey로부터 projectKey 조회)
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = erdTableService.getProjectKeyByTableKey(tableKey);
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        ErdTableResponse table = erdTableService.updateTable(tableKey, request, userKey);

        // Kafka 브로드캐스트 (변경된 필드만)
        if (request.getLogicalName() != null) {
            broadcast(TableLnameEvent.builder()
                    .tableKey(tableKey)
                    .logicalName(table.getLogicalName())
                    .build());
        }
        if (request.getPhysicalName() != null) {
            broadcast(TablePnameEvent.builder()
                    .tableKey(tableKey)
                    .physicalName(table.getPhysicalName())
                    .build());
        }
        if (request.getColorHex() != null) {
            broadcast(TableColorEvent.builder()
                    .tableKey(tableKey)
                    .colorHex(table.getColorHex())
                    .build());
        }

        return ApiResponse.onSuccess(table);
    }

    /**
     * ERD 테이블 삭제
     */
    @Operation(summary = "ERD 테이블 삭제", description = "ERD 테이블을 삭제합니다.")
    @DeleteMapping("/tables/{tableKey}")
    public ApiResponse<Void> deleteTable(@PathVariable Long tableKey) {
        // 프로젝트 접근 권한 검증 (tableKey로부터 projectKey 조회)
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = erdTableService.getProjectKeyByTableKey(tableKey);
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        // Kafka 브로드캐스트 (최소 정보)
        broadcast(TableDelEvent.builder()
                .tableKey(tableKey)
                .build());

        erdTableService.deleteTable(tableKey, userKey);


        return ApiResponse.onSuccess(null);
    }

    // ========== ERD Column API ==========

    /**
     * ERD 컬럼 생성
     */
    @Operation(summary = "ERD 컬럼 생성", description = "새로운 ERD 컬럼을 생성합니다.")
    @PostMapping("/tables/{tableKey}/columns")
    public ApiResponse<ErdColumnResponse> createColumn(
            @PathVariable Long tableKey,
            @Valid @RequestBody ErdColumnCreateRequest request) {
        // 프로젝트 접근 권한 검증 (tableKey로부터 projectKey 조회)
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = erdTableService.getProjectKeyByTableKey(tableKey);
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        ErdColumnResponse column = erdColumnService.createColumn(tableKey, request, userKey);

        // Kafka 브로드캐스트
        broadcast(ColumnNewEvent.from(column));

        return ApiResponse.onSuccess(column);
    }

    /**
     * ERD 컬럼 수정
     */
    @Operation(summary = "ERD 컬럼 수정", description = "ERD 컬럼을 수정합니다.")
    @PatchMapping("/columns/{columnKey}")
    public ApiResponse<ErdColumnResponse> updateColumn(
            @PathVariable Long columnKey,
            @Valid @RequestBody ErdColumnUpdateRequest request) {
        // 프로젝트 접근 권한 검증 (columnKey로부터 projectKey 조회)
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = erdColumnService.getProjectKeyByColumnKey(columnKey);
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        ErdColumnResponse column = erdColumnService.updateColumn(columnKey, request, userKey);

        // Kafka 브로드캐스트
        broadcast(ColumnUpdateEvent.from(column));

        return ApiResponse.onSuccess(column);
    }

    /**
     * ERD 컬럼 삭제
     */
    @Operation(summary = "ERD 컬럼 삭제", description = "ERD 컬럼을 삭제합니다.")
    @DeleteMapping("/columns/{columnKey}")
    public ApiResponse<Void> deleteColumn(@PathVariable Long columnKey) {
        // 프로젝트 접근 권한 검증 (columnKey로부터 projectKey 조회)
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = erdColumnService.getProjectKeyByColumnKey(columnKey);
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        // Kafka 브로드캐스트
        broadcast(ColumnDelEvent.builder()
                .columnKey(columnKey)
                .build());

        erdColumnService.deleteColumn(columnKey, userKey);

        return ApiResponse.onSuccess(null);
    }

    // ========== ERD Relation API ==========

    /**
     * ERD 관계 생성
     */
    @Operation(summary = "ERD 관계 생성", description = "새로운 ERD 관계를 생성합니다.")
    @PostMapping("/projects/{projectKey}/relations")
    public ApiResponse<ErdRelationResponse> createRelation(
            @PathVariable Long projectKey,
            @Valid @RequestBody ErdRelationCreateRequest request) {
        // 프로젝트 접근 권한 검증
        Integer userKey = SecurityUtil.getCurrentUserKey();
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        ErdRelationWithFkResponse response = erdRelationService.createRelation(projectKey, request, userKey);

        // Kafka 브로드캐스트
        broadcast(RelationNewEvent.from(response.getErdRelationResponse()));

        List<ErdColumnResponse> columns = response.getColumns();

        for(ErdColumnResponse column : columns) {
            broadcast(ColumnNewEvent.from(column));
        }
        return ApiResponse.onSuccess(response.getErdRelationResponse());
    }

    /**
     * ERD 관계 수정
     */
    @Operation(summary = "ERD 관계 수정", description = "ERD 관계를 수정합니다.")
    @PatchMapping("/relations/{relationKey}")
    public ApiResponse<ErdRelationResponse> updateRelation(
            @PathVariable Long relationKey,
            @Valid @RequestBody ErdRelationUpdateRequest request) {
        // 프로젝트 접근 권한 검증 (relationKey로부터 projectKey 조회)
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = erdRelationService.getProjectKeyByRelationKey(relationKey);
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        ErdRelationResponse relation = erdRelationService.updateRelation(relationKey, request, userKey);

        // Kafka 브로드캐스트
        broadcast(RelationUpdateEvent.from(relation));

        return ApiResponse.onSuccess(relation);
    }

    /**
     * ERD 관계 삭제
     */
    @Operation(summary = "ERD 관계 삭제", description = "ERD 관계를 삭제합니다.")
    @DeleteMapping("/relations/{relationKey}")
    public ApiResponse<Void> deleteRelation(@PathVariable Long relationKey) {
        // 프로젝트 접근 권한 검증 (relationKey로부터 projectKey 조회)
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = erdRelationService.getProjectKeyByRelationKey(relationKey);
        projectAccessValidator.validateProjectAccess(projectKey, userKey);

        erdRelationService.deleteRelation(relationKey, userKey);

        // Kafka 브로드캐스트
        broadcast(RelationDelEvent.builder()
                .relationKey(relationKey)
                .build());

        return ApiResponse.onSuccess(null);
    }

    /**
     * Kafka 브로드캐스트 헬퍼 메서드
     */
    private void broadcast(WebSocketEvent event) {
        Integer userKey = SecurityUtil.getCurrentUserKey();
        Long projectKey = getProjectKeyFromEvent(event);

        ErdBroadcastEvent broadcastEvent = ErdBroadcastEvent.builder()
                .projectKey(projectKey)
                .userKey(userKey)
                .event(event)
                .build();

        messagingTemplate.convertAndSend("/topic/project/" + projectKey, broadcastEvent);
    }

    /**
     * 이벤트에서 projectKey 추출
     */
    private Long getProjectKeyFromEvent(WebSocketEvent event) {
        return switch (event) {
            // Table 이벤트 - projectKey 직접 보유
            case TableNewEvent e -> e.getProjectKey();

            // Table 이벤트 - tableKey로부터 조회
            case TableMoveEvent e -> erdTableService.getProjectKeyByTableKey(e.getTableKey());
            case TableLnameEvent e -> erdTableService.getProjectKeyByTableKey(e.getTableKey());
            case TablePnameEvent e -> erdTableService.getProjectKeyByTableKey(e.getTableKey());
            case TableColorEvent e -> erdTableService.getProjectKeyByTableKey(e.getTableKey());
            case TableDelEvent e -> erdTableService.getProjectKeyByTableKey(e.getTableKey());

            // Column 이벤트
            case ColumnNewEvent e -> erdTableService.getProjectKeyByTableKey(e.getTableKey());
            case ColumnUpdateEvent e -> erdColumnService.getProjectKeyByColumnKey(e.getColumnKey());
            case ColumnDelEvent e -> erdColumnService.getProjectKeyByColumnKey(e.getColumnKey());

            // Relation 이벤트
            case RelationNewEvent e -> erdRelationService.getProjectKeyByRelationKey(e.getRelationKey());
            case RelationUpdateEvent e -> erdRelationService.getProjectKeyByRelationKey(e.getRelationKey());
            case RelationDelEvent e -> erdRelationService.getProjectKeyByRelationKey(e.getRelationKey());

            default -> throw new GeneralException(ErrorStatus.WEBSOCKET_UNSUPPORTED_EVENT);
        };
    }
}
