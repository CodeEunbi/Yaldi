package com.yaldi.domain.consultation.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationAiResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("schema_modifications")
    private List<Map<String, Object>> schemaModifications;

    @JsonProperty("confidence")
    private Float confidence;

    @JsonProperty("agents_used")
    private List<String> agentsUsed;

    @JsonProperty("warnings")
    private List<String> warnings;
}
