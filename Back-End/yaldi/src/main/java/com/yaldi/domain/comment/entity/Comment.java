package com.yaldi.domain.comment.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Comment 엔티티
 */
@Entity
@Table(name = "comments")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_key")
    private Long commentKey;

    @Column(name = "user_key", nullable = false)
    private Integer userKey;

    @Column(name = "table_key")
    private Long tableKey;

    @Column(name = "project_key")
    private Long projectKey;

    @Column(name = "content", length = 1000, nullable = false)
    @Builder.Default
    private String content = "";

    @Column(name = "color_hex", length = 6)
    private String colorHex;

    @Column(name = "x_position", precision = 10, scale = 2)
    private BigDecimal xPosition;

    @Column(name = "y_position", precision = 10, scale = 2)
    private BigDecimal  yPosition;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private Boolean isResolved = false;

    // 비즈니스 로직
    public void updateContent(String content) {
        this.content = content;
    }

    public void resolve() {
        this.isResolved = true;
    }

    public void unresolve() {
        this.isResolved = false;
    }
}
