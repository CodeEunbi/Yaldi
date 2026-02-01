package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 테이블 생성 이벤트 (전체 데이터)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("TABLE_CREATED")
public class TableNewEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "TABLE_NEW";

    @JsonProperty("tableKey")
    private Long tableKey;

    @JsonProperty("projectKey")
    private Long projectKey;

    @JsonProperty("logicalName")
    private String logicalName;

    @JsonProperty("physicalName")
    private String physicalName;

    @JsonProperty("xPosition")
    private BigDecimal xPosition;

    @JsonProperty("yPosition")
    private BigDecimal yPosition;

    @JsonProperty("colorHex")
    private String colorHex;

    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;
}
