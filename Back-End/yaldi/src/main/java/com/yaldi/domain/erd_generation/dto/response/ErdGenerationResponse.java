package com.yaldi.domain.erd_generation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErdGenerationResponse {

    // @JsonProperty 어노테이션은 Jackson 라이브러리에서 Java 필드명과 JSON 키 이름을 매핑할 때 사용

    //생성 모드: REFERENCE (유사 프로젝트 기반) / ZERO_BASE (신규 설계)
    private String mode;

    //유사도 점수 (0.0 ~ 1.0)
    @JsonProperty("similarity_score")
    private Double similarityScore;

    @JsonProperty("similar_projects")
    private List<Map<String, Object>> similarProjects;

    @JsonProperty("generated_schema")
    private Map<String, Object> generatedSchema;

    @JsonProperty("sql_script")
    private String sqlScript;

    private String explanation;

    //Agent들의 사고 과정
    @JsonProperty("agent_thoughts")
    private List<AgentThought> agentThoughts;

    @JsonProperty("validation_report")
    private Map<String, Object> validationReport;

    @JsonProperty("optimization_suggestions")
    private Map<String, Object> optimizationSuggestions;

    @JsonProperty("execution_time_ms")
    private Integer executionTimeMs;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentThought {
        private String step;
        private String timestamp;
        private String result;
    }
}
