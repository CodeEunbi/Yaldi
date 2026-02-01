package com.yaldi.domain.erd.dto.websocket.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.yaldi.domain.erd.dto.response.ErdColumnResponse;
import com.yaldi.infra.websocket.dto.WebSocketEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Column create event (full data included)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonTypeName("COLUMN_CREATED")
public class ColumnNewEvent implements WebSocketEvent {
    @JsonProperty("type")
    private final String type = "COLUMN_NEW";

    @JsonProperty("columnKey")
    private Long columnKey;

    @JsonProperty("tableKey")
    private Long tableKey;

    @JsonProperty("logicalName")
    private String logicalName;

    @JsonProperty("physicalName")
    private String physicalName;

    @JsonProperty("dataType")
    private String dataType;

    @JsonProperty("dataDetail")
    private String[] dataDetail;

    @JsonProperty("isNullable")
    private Boolean isNullable;

    @JsonProperty("isPrimaryKey")
    private Boolean isPrimaryKey;

    @JsonProperty("isForeignKey")
    private Boolean isForeignKey;

    @JsonProperty("isUnique")
    private Boolean isUnique;

    @JsonProperty("isIncremental")
    private Boolean isIncremental;

    @JsonProperty("defaultValue")
    private String defaultValue;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("columnOrder")
    private Integer columnOrder;

    public static ColumnNewEvent from(ErdColumnResponse response) {
        return ColumnNewEvent.builder()
                .columnKey(response.getColumnKey())
                .tableKey(response.getTableKey())
                .logicalName(response.getLogicalName())
                .physicalName(response.getPhysicalName())
                .dataType(response.getDataType())
                .dataDetail(response.getDataDetail())
                .isNullable(response.getIsNullable())
                .isPrimaryKey(response.getIsPrimaryKey())
                .isForeignKey(response.getIsForeignKey())
                .isUnique(response.getIsUnique())
                .isIncremental(response.getIsIncremental())
                .defaultValue(response.getDefaultValue())
                .comment(response.getComment())
                .columnOrder(response.getColumnOrder())
                .build();
    }
}
