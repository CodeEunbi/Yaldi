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
 * Relation create event (full data included)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("RELATION_CREATED")
public class RelationNewEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "RELATION_NEW";

    @JsonProperty("relationKey")
    private Long relationKey;

    @JsonProperty("fromTableKey")
    private Long fromTableKey;

    @JsonProperty("fromColumnKey")
    private Long fromColumnKey;

    @JsonProperty("toTableKey")
    private Long toTableKey;

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

    public static RelationNewEvent from(ErdRelationResponse response) {
        return RelationNewEvent.builder()
                .relationKey(response.getRelationKey())
                .fromTableKey(response.getFromTableKey())
                .fromColumnKey(response.getFromColumnKey())
                .toTableKey(response.getToTableKey())
                .toColumnKey(response.getFromColumnKey())
                .relationType(response.getRelationType())
                .constraintName(response.getConstraintName())
                .onDeleteAction(response.getOnDeleteAction())
                .onUpdateAction(response.getOnUpdateAction())
                .build();
    }
}
