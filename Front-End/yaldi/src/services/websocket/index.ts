/**
 * ERD WebSocket 서비스 진입점
 * 모든 WebSocket 관련 클래스와 타입을 export
 */

// 메인 클라이언트
export { ErdWebSocketClient } from './ErdWebSocketClient';

// 핸들러
export { TableDragHandler } from './handlers/TableDragHandler';
export { CursorSharingHandler } from './handlers/CursorSharingHandler';
export { TableLockHandler } from './handlers/TableLockHandler';

// 타입
export type {
  EventType,
  TableMoveEvent,
  TableMoveEndEvent,
  ColumnReorderEvent,
  TableLockEvent,
  TableUnlockEvent,
  CursorMoveEvent,
  ErdEvent,
  ErdBroadcastEvent,
  WebSocketResponse,
  LockInfo,
  RemoteCursor,
  WebSocketOptions,
  EventHandlers,
} from './types';

// 핸들러 옵션 타입
export type { Position, TableDragOptions } from './handlers/TableDragHandler';
export type { CursorSharingOptions } from './handlers/CursorSharingHandler';
export type { TableLockOptions } from './handlers/TableLockHandler';
