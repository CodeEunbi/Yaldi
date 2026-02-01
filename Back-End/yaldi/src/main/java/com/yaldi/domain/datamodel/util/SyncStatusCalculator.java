package com.yaldi.domain.datamodel.util;

import com.yaldi.domain.datamodel.entity.SyncStatus;

import java.time.OffsetDateTime;

/**
 * 데이터 모델 동기화 상태 계산 유틸리티
 *
 * <p>데이터 모델의 동기화 상태(SyncStatus)를 계산합니다.</p>
 *
 * <p>계산 로직:</p>
 * <ul>
 *   <li>INVALID: 관련 컬럼이 삭제된 경우 (Refresh 불가, 재생성 필요)</li>
 *   <li>OUT_OF_SYNC: 마지막 동기화 이후 ERD가 변경된 경우</li>
 *   <li>IN_SYNC: ERD와 동기화된 상태</li>
 * </ul>
 */
public class SyncStatusCalculator {

    /**
     * 동기화 상태 계산
     *
     * @param lastSyncedAt 마지막 동기화 시각 (null 가능)
     * @param hasDeletedColumns 삭제된 컬럼 존재 여부
     * @param lastErdUpdatedAt ERD의 마지막 업데이트 시각 (테이블/컬럼 중 최신, null 가능)
     * @return 계산된 SyncStatus
     */
    public static SyncStatus calculate(
            OffsetDateTime lastSyncedAt,
            boolean hasDeletedColumns,
            OffsetDateTime lastErdUpdatedAt
    ) {
        // 1. 관련 컬럼이 삭제됨 → INVALID
        if (hasDeletedColumns) {
            return SyncStatus.INVALID;
        }

        // 2. 한번도 동기화하지 않음 → OUT_OF_SYNC
        if (lastSyncedAt == null) {
            return SyncStatus.OUT_OF_SYNC;
        }

        // 3. ERD 업데이트 시각이 없으면 (데이터 없음) → IN_SYNC로 간주
        if (lastErdUpdatedAt == null) {
            return SyncStatus.IN_SYNC;
        }

        // 4. ERD가 마지막 동기화 이후 변경됨 → OUT_OF_SYNC
        if (lastErdUpdatedAt.isAfter(lastSyncedAt)) {
            return SyncStatus.OUT_OF_SYNC;
        }

        // 5. 그 외 → IN_SYNC
        return SyncStatus.IN_SYNC;
    }

    /**
     * 동기화 상태 메시지 생성
     *
     * @param syncStatus 동기화 상태
     * @return 사용자에게 표시할 메시지
     */
    public static String getMessage(SyncStatus syncStatus) {
        return switch (syncStatus) {
            case IN_SYNC -> "동기화됨";
            case OUT_OF_SYNC -> "변경사항 있음";
            case INVALID -> "관련 컬럼이 삭제되어 무효 상태입니다. 재생성이 필요합니다.";
        };
    }
}
