package com.yaldi.domain.datamodel.dto.response;

import com.yaldi.domain.datamodel.entity.DataModelType;
import com.yaldi.domain.datamodel.entity.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 데이터 모델 응답 DTO (목록 조회용)
 */
@Schema(description = "데이터 모델 응답")
public record DataModelResponse(

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

        @Schema(description = "관련 테이블 목록")
        List<TableInfo> relatedTables,

        @Schema(description = "포함된 컬럼 개수", example = "5")
        Integer columnCount,

        @Schema(description = "생성 시각")
        OffsetDateTime createdAt,

        @Schema(description = "수정 시각")
        OffsetDateTime updatedAt
) {
    /**
     * 테이블 정보
     */
    @Schema(description = "테이블 정보")
    public record TableInfo(
            @Schema(description = "테이블 키")
            Long tableKey,

            @Schema(description = "물리명", example = "users")
            String physicalName,

            @Schema(description = "논리명", example = "사용자")
            String logicalName
    ) {
        /**
         * 표시용 이름: "users (사용자)"
         */
        public String getDisplayName() {
            if (logicalName != null && !logicalName.isBlank()) {
                return physicalName + " (" + logicalName + ")";
            }
            return physicalName;
        }
    }
}
