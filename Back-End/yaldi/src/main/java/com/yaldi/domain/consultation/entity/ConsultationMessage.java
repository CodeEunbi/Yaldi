package com.yaldi.domain.consultation.entity;

import com.yaldi.global.common.BaseCreateOnlyEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "consultation_messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationMessage extends BaseCreateOnlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_key")
    private Long messageKey;

    @Column(name = "project_key", nullable = false)
    private Long projectKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false, columnDefinition = "consultation_message_role_type")
    private ConsultationMessageRole role;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * AI 응답인 경우: 적용 가능한 스키마 수정사항
     * JSON 배열 형태
     * [{"action": "ADD_INDEX", "table": "User", "column": "email", ...}, ...]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "schema_modifications", columnDefinition = "jsonb")
    private List<Map<String, Object>> schemaModifications;

    //AI 응답인 경우: 확신도 (0.0 ~ 1.0)
    @Column(name = "confidence")
    private Float confidence;

    //AI 응답인 경우: 사용된 Agent 목록
    //JSON 배열 형태: ["NormalizationExpert", "IndexStrategyExpert"]
    @Type(JsonBinaryType.class)
    @Column(name = "agents_used", columnDefinition = "jsonb")
    private List<String> agentsUsed;

    /**
     * AI 응답인 경우: 경고 사항
     * JSON 배열 형태: ["기존 쿼리 수정 필요", ...]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "warnings", columnDefinition = "jsonb")
    private List<String> warnings;

    /**
     * 요청 시점의 스키마 스냅샷 (선택적)
     * 나중에 "이때 뭐 물어봤더라" 재현용
     */
    @Type(JsonBinaryType.class)
    @Column(name = "schema_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> schemaSnapshot;
}
