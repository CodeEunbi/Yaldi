package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.domain.erd.dto.response.ErdRelationResponse;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import com.yaldi.domain.erd.entity.ReferentialActionType;
import com.yaldi.domain.erd.entity.RelationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Relation update event (updatable fields only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("RELATION_UPDATE")
public class RelationUpdateEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "RELATION_UPDATE";

    @JsonProperty("relationKey")
    private Long relationKey;

    @JsonProperty("fromColumnKey")
    private Long fromColumnKey;

    @JsonProperty("toColumnKey")
    private Long toColumnKey;

    @JsonProperty("relationType")
    private RelationType relationType;

    @JsonProperty("constraintName")
    private String constraintName;

    @JsonProperty("onDeleteAction")
    private ReferentialActionType onDeleteAction;

    @JsonProperty("onUpdateAction")
    private ReferentialActionType onUpdateAction;

    public static RelationUpdateEvent from(ErdRelationResponse response) {
        return RelationUpdateEvent.builder()
                .relationKey(response.getRelationKey())
                .fromColumnKey(response.getFromColumnKey())
                .relationType(response.getRelationType())
                .constraintName(response.getConstraintName())
                .onDeleteAction(response.getOnDeleteAction())
                .onUpdateAction(response.getOnUpdateAction())
                .build();
    }
}
