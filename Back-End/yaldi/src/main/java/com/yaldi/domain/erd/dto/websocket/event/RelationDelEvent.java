package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Relation delete event (key only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("RELATION_DELETED")
public class RelationDelEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "RELATION_DEL";

    @JsonProperty("relationKey")
    private Long relationKey;
}
