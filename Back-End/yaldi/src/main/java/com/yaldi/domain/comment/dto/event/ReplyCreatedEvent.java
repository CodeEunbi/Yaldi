package com.yaldi.domain.comment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("REPLY_CREATED")
public class ReplyCreatedEvent implements WebSocketEvent {

    @JsonProperty("replyKey")
    private Long replyKey;

    @JsonProperty("commentKey")
    private Long commentKey;

    @JsonProperty("projectKey")
    private Long projectKey;

    @JsonProperty("userKey")
    private Integer userKey;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("content")
    private String content;

    @Override
    public String getType() {
        return "REPLY_CREATED";
    }
}