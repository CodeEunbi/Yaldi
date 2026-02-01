package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 멤버 입장 이벤트
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("MEMBER_JOIN")
public class MemberJoinEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "MEMBER_JOIN";

    @JsonProperty("projectKey")
    private Long projectKey;

    @JsonProperty("userEmail")
    private String userEmail;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("userColor")
    private String userColor;
}
