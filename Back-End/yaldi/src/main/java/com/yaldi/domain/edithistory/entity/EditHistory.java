package com.yaldi.domain.edithistory.entity;

import com.yaldi.global.common.BaseAuditEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * EditHistory 엔티티
 */
@Entity
@Table(name = "edit_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditHistory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edit_history_key")
    private Long editHistoryKey;

    @Column(name = "user_key", nullable = false)
    private Integer userKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Column(name = "target_key")
    private Long targetKey;

    @Convert(converter = EditHistoryTargetTypeConverter.class)
    @Column(name = "target_type", length = 50)
    private EditHistoryTargetType targetType;

    @Convert(converter = EditHistoryActionTypeConverter.class)
    @Column(name = "action_type", length = 50, nullable = false)
    private EditHistoryActionType actionType;

    @Type(JsonBinaryType.class)
    @Column(name = "delta", columnDefinition = "jsonb")
    private Map<String, Object> delta;

    @Type(JsonBinaryType.class)
    @Column(name = "before_state", columnDefinition = "jsonb")
    private Map<String, Object> beforeState;

    @Type(JsonBinaryType.class)
    @Column(name = "after_state", columnDefinition = "jsonb")
    private Map<String, Object> afterState;
}
