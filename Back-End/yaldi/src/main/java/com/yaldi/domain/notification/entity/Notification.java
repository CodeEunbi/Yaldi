package com.yaldi.domain.notification.entity;

import com.yaldi.global.common.BaseCreateOnlyEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Notification 엔티티
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseCreateOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_key")
    private Long notificationKey;

    @Column(name = "user_key", nullable = false)
    private Integer userKey;

    /**
     * 팀,프로젝트에 추가됨
     * 팀,프로젝트에서방출됨
     * 내가 프젝,팀오너
     * 새버전 생성
     */
    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String content = "";

    @Column(name = "target")
    private Long target;


    @Column(name = "read_at")
    private OffsetDateTime readAt;

    // 비즈니스 로직
    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = OffsetDateTime.now();;
        }
    }

    public void changeType(String type) {
        this.type = type;
    }
}
