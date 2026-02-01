package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Table unlock event (WebSocket + Redis)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("TABLE_UNLOCK")
public class TableUnlockEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "TABLE_UNLOCK";

    @JsonProperty("tableKey")
    private Long tableKey;

    @JsonProperty("userEmail")
    private String userEmail;
}
