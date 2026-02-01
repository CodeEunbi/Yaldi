package com.yaldi.domain.comment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("REPLY_DELETED")
public class ReplyDeletedEvent implements WebSocketEvent {

    @JsonProperty("replyKey")
    private Long replyKey;

    @JsonProperty("commentKey")
    private Long commentKey;

    @JsonProperty("projectKey")
    private Long projectKey;

    @Override
    public String getType() {
        return "REPLY_DELETED";
    }
}