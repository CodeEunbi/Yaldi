package com.yaldi.domain.comment.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Reply 엔티티
 */
@Entity
@Table(name = "replies")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reply extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reply_key")
    private Long replyKey;

    @Column(name = "comment_key", nullable = false)
    private Long commentKey;

    @Column(name = "user_key", nullable = false)
    private Integer userKey;

    @Column(name = "content", length = 1000, nullable = false)
    @Builder.Default
    private String content = "";

    // 비즈니스 로직
    public void updateContent(String content) {
        this.content = content;
    }
}
