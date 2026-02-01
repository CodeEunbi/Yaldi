package com.yaldi.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * 생성 및 수정 시간을 기록하는 Base Entity
 * 사용 케이스: 생성/수정 이력 추적이 필요하지만 소프트 삭제가 불필요한 엔티티 (관계, 버전, data_model_erd_column_relations 등)
 *
 * <p>OffsetDateTime을 사용하여 PostgreSQL의 timestamptz 타입과 완벽하게 매칭됩니다.</p>
 */
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
