package com.yaldi.infra.websocket.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.yaldi.domain.comment.dto.event.CommentCreatedEvent;
import com.yaldi.domain.comment.dto.event.CommentDeletedEvent;
import com.yaldi.domain.comment.dto.event.CommentResolvedEvent;
import com.yaldi.domain.comment.dto.event.ReplyCreatedEvent;
import com.yaldi.domain.comment.dto.event.ReplyDeletedEvent;
import com.yaldi.domain.erd.dto.websocket.event.ColumnDelEvent;
import com.yaldi.domain.erd.dto.websocket.event.ColumnNewEvent;
import com.yaldi.domain.erd.dto.websocket.event.ColumnOrderEvent;
import com.yaldi.domain.erd.dto.websocket.event.ColumnUpdateEvent;
import com.yaldi.domain.erd.dto.websocket.event.CursorPosEvent;
import com.yaldi.domain.erd.dto.websocket.event.MemberJoinEvent;
import com.yaldi.domain.erd.dto.websocket.event.MemberLeaveEvent;
import com.yaldi.domain.erd.dto.websocket.event.RelationDelEvent;
import com.yaldi.domain.erd.dto.websocket.event.RelationNewEvent;
import com.yaldi.domain.erd.dto.websocket.event.RelationUpdateEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableColorEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableDelEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableLnameEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableLockEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableMoveEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableNewEvent;
import com.yaldi.domain.erd.dto.websocket.event.TablePnameEvent;
import com.yaldi.domain.erd.dto.websocket.event.TableUnlockEvent;

/**
 * WebSocket 이벤트 기본 인터페이스
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ColumnDelEvent.class, name = "COLUMN_DELETED"),
        @JsonSubTypes.Type(value = ColumnNewEvent.class, name = "COLUMN_CREATED"),
        @JsonSubTypes.Type(value = ColumnOrderEvent.class, name = "COLUMN_ORDER"),
        @JsonSubTypes.Type(value = ColumnUpdateEvent.class, name = "COLUMN_UPDATED"),
        @JsonSubTypes.Type(value = CommentCreatedEvent.class, name = "COMMENT_CREATED"),
        @JsonSubTypes.Type(value = CommentDeletedEvent.class, name = "COMMENT_DELETED"),
        @JsonSubTypes.Type(value = CommentResolvedEvent.class, name = "COMMENT_RESOLED"),
        @JsonSubTypes.Type(value = CursorPosEvent.class, name = "CURSOR_POS"),
        @JsonSubTypes.Type(value = MemberJoinEvent.class, name = "MEMBER_JOIN"),
        @JsonSubTypes.Type(value = MemberLeaveEvent.class, name = "MEMBER_LEAVE"),
        @JsonSubTypes.Type(value = RelationDelEvent.class, name = "RELATION_DELETED"),
        @JsonSubTypes.Type(value = RelationNewEvent.class, name = "RELATION_CREATED"),
        @JsonSubTypes.Type(value = RelationUpdateEvent.class, name = "RELATION_UPDATE"),
        @JsonSubTypes.Type(value = ReplyCreatedEvent.class, name = "REPLY_CREATED"),
        @JsonSubTypes.Type(value = ReplyDeletedEvent.class, name = "REPLY_DELETED"),
        @JsonSubTypes.Type(value = TableColorEvent.class, name = "TABLE_COLOR"),
        @JsonSubTypes.Type(value = TableDelEvent.class, name = "TABLE_DELETED"),
        @JsonSubTypes.Type(value = TableLnameEvent.class, name = "TABLE_LNAME"),
        @JsonSubTypes.Type(value = TableLockEvent.class, name = "TABLE_LOCK"),
        @JsonSubTypes.Type(value = TableMoveEvent.class, name = "TABLE_MOVE"),
        @JsonSubTypes.Type(value = TableNewEvent.class, name = "TABLE_CREATED"),
        @JsonSubTypes.Type(value = TablePnameEvent.class, name = "TABLE_PNAME"),
        @JsonSubTypes.Type(value = TableUnlockEvent.class, name = "TABLE_UNLOCK")

})
public interface WebSocketEvent {
    String getType();
}
