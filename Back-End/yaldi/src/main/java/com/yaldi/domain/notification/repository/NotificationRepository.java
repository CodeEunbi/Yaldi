package com.yaldi.domain.notification.repository;

import com.yaldi.domain.notification.entity.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자의 알림 목록 조회 (최신순)
     */
    List<Notification> findByUserKeyOrderByCreatedAtDesc(Integer userKey);

    Optional<Notification> findByTypeAndUserKeyAndTarget(String type, Integer userKey, Long target);
    /**
     * 사용자의 읽지 않은 알림 목록 조회
     */
    List<Notification> findByUserKeyAndReadAtIsNullOrderByCreatedAtDesc(Integer userKey);

    /**
     * 사용자의 읽지 않은 알림 개수
     */
    long countByUserKeyAndReadAtIsNull(Integer userKey);

    Page<Notification> findByUserKeyOrderByCreatedAtDesc(Integer userKey, Pageable pageable);

    /**
     * 특정 타입의 알림 조회
     */
    List<Notification> findByUserKeyAndTypeOrderByCreatedAtDesc(Integer userKey, String type);

}
