package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Column delete event (key only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("COLUMN_DELETED")
public class ColumnDelEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "COLUMN_DEL";

    @JsonProperty("columnKey")
    private Long columnKey;
}
