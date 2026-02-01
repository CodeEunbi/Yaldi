package com.yaldi.domain.comment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("COMMENT_DELETED")
public class CommentDeletedEvent implements WebSocketEvent {

    @JsonProperty("commentKey")
    private Long commentKey;

    @JsonProperty("projectKey")
    private Long projectKey;

    @Override
    public String getType() {
        return "COMMENT_DELETED";
    }
}