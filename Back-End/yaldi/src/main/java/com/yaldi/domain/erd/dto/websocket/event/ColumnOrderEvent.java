package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Column order change event (WebSocket Only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("COLUMN_ORDER")
public class ColumnOrderEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "COLUMN_ORDER";

    @JsonProperty("columnKey")
    private Long columnKey;

    @JsonProperty("columnOrder")
    private Integer columnOrder;
}
