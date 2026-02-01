package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cursor position event (WebSocket Only, fully volatile)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("CURSOR_POS")
public class CursorPosEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "CURSOR_POS";

    @JsonProperty("projectKey")
    private Long projectKey;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("userColor")
    private String userColor;

    @JsonProperty("xPosition")
    private BigDecimal xPosition;

    @JsonProperty("yPosition")
    private BigDecimal yPosition;
}
