package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 테이블 논리명 변경 이벤트
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("TABLE_LNAME")
public class TableLnameEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "TABLE_LNAME";

    @JsonProperty("tableKey")
    private Long tableKey;

    @JsonProperty("logicalName")
    private String logicalName;
}
