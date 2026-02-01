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
 * 생성, 수정, 삭제 시간을 기록하는 Base Entity (소프트 삭제 지원)
 * 사용 케이스: 데이터 복구 가능성이 필요하거나 삭제 이력을 유지해야 하는 중요 엔티티 (사용자, 프로젝트, ERD 등)
 *
 * <p>OffsetDateTime을 사용하여 PostgreSQL의 timestamptz 타입과 완벽하게 매칭됩니다.
 * DB에는 UTC로 저장되며, 클라이언트의 타임존에 따라 응답 시 변환됩니다.</p>
 */
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseSoftDeleteEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void restore() {
        this.deletedAt = null;
    }
}
