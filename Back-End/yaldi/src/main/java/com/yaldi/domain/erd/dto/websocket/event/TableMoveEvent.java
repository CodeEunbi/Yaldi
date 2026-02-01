package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.*;

import java.math.BigDecimal;

/**
 * 테이블 이동 이벤트
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonTypeName("TABLE_MOVE")
public class TableMoveEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "TABLE_MOVE";

    @JsonProperty("tableKey")
    private Long tableKey;

    @JsonProperty("xPosition")
    private BigDecimal xPosition;

    @JsonProperty("yPosition")
    private BigDecimal yPosition;
}
