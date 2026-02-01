package com.yaldi.domain.datamodel.dto.response;

import com.yaldi.domain.datamodel.entity.DataModelType;
import com.yaldi.domain.datamodel.entity.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 데이터 모델 상세 응답 DTO (생성된 코드 포함)
 * 상세 조회 시 사용 - Java/TypeScript 코드 포함
 */
@Schema(description = "데이터 모델 상세 응답")
public record DataModelDetailResponse(

        @Schema(description = "모델 키", example = "1")
        Long modelKey,

        @Schema(description = "프로젝트 키", example = "10")
        Long projectKey,

        @Schema(description = "모델명", example = "UserEntity")
        String name,

        @Schema(description = "모델 타입", example = "ENTITY")
        DataModelType type,

        @Schema(description = "동기화 상태", example = "IN_SYNC")
        SyncStatus syncStatus,

        @Schema(description = "동기화 메시지", example = "동기화됨")
        String syncMessage,

        @Schema(description = "마지막 동기화 시각")
        OffsetDateTime lastSyncedAt,

        @Schema(description = "생성된 코드 (언어별)")
        Map<String, String> code,

        @Schema(description = "포함된 컬럼 상세 정보")
        List<ColumnDetailDto> columns,

        @Schema(description = "관련 테이블 정보")
        List<DataModelResponse.TableInfo> relatedTables,

        @Schema(description = "생성 시각")
        OffsetDateTime createdAt,

        @Schema(description = "수정 시각")
        OffsetDateTime updatedAt
) {
    /**
     * 컬럼 상세 정보
     */
    @Schema(description = "컬럼 상세 정보")
    public record ColumnDetailDto(
            @Schema(description = "컬럼 키")
            Long columnKey,

            @Schema(description = "테이블 키")
            Long tableKey,

            @Schema(description = "테이블 물리명", example = "users")
            String tableName,

            @Schema(description = "논리명", example = "사용자키")
            String logicalName,

            @Schema(description = "물리명", example = "user_key")
            String physicalName,

            @Schema(description = "데이터 타입", example = "BIGINT")
            String dataType,

            @Schema(description = "데이터 타입 상세", example = "[\"255\"]")
            String[] dataDetail,

            @Schema(description = "NULL 허용 여부")
            Boolean isNullable,

            @Schema(description = "Primary Key 여부")
            Boolean isPrimaryKey,

            @Schema(description = "Foreign Key 여부")
            Boolean isForeignKey,

            @Schema(description = "UNIQUE 제약 여부")
            Boolean isUnique,

            @Schema(description = "별칭 (DTO 생성 시 사용)", example = "userId", nullable = true)
            String alias
    ) {
    }
}