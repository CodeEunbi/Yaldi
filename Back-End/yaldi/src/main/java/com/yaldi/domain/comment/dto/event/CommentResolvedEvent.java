package com.yaldi.domain.comment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("COMMENT_RESOLED")
public class CommentResolvedEvent implements WebSocketEvent {

    @JsonProperty("commentKey")
    private Long commentKey;

    @JsonProperty("projectKey")
    private Long projectKey;

    @JsonProperty("resolved")
    private Boolean resolved;   // true: 해결, false: 취소

    @Override
    public String getType() {
        return "COMMENT_RESOLVED";
    }
}