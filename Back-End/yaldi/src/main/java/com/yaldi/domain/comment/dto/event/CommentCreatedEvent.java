package com.yaldi.domain.comment.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("COMMENT_CREATED")
public class CommentCreatedEvent implements WebSocketEvent {

    @JsonProperty("commentKey")
    private Long commentKey;

    @JsonProperty("projectKey")
    private Long projectKey;

    @JsonProperty("tableKey")
    private Long tableKey;

    @JsonProperty("userKey")
    private Integer userKey;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("content")
    private String content;

    @JsonProperty("colorHex")
    private String colorHex;

    @JsonProperty("xPosition")
    private BigDecimal xPosition;

    @JsonProperty("yPosition")
    private BigDecimal yPosition;

    @JsonProperty("isResolved")
    private Boolean isResolved;

    @Override
    public String getType() {
        return "COMMENT_CREATED";
    }
}
