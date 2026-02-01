package com.yaldi.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * 생성 시간만 기록하는 Base Entity
 * 사용 케이스: 생성 후 변경되지 않는 불변 데이터 (히스토리, 알림 등)
 * 참고: data_model_erd_column_relations는 수정일시 추가로 BaseAuditEntity 사용
 *
 * <p>OffsetDateTime을 사용하여 PostgreSQL의 timestamptz 타입과 완벽하게 매칭됩니다.</p>
 */
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseCreateOnlyEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
