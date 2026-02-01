package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 테이블 삭제 이벤트
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("TABLE_DELETED")
public class TableDelEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "TABLE_DEL";

    @JsonProperty("tableKey")
    private Long tableKey;
}
