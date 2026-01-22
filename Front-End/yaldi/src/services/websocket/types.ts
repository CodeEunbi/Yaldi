/**
 * WebSocket 이벤트 타입 정의
 */

// 이벤트 타입
export type EventType =
  | 'TABLE_MOVE'           // 테이블 드래그 중
  | 'TABLE_MOVE_END'       // 테이블 드래그 완료
  | 'COLUMN_REORDER'       // 컬럼 순서 변경
  | 'TABLE_LOCK'           // 테이블 편집 락
  | 'TABLE_UNLOCK'         // 테이블 편집 언락
  | 'CURSOR_MOVE'          // 커서 위치 공유
  | 'TABLE_CREATE'         // 테이블 생성 (REST API 후 브로드캐스트)
  | 'TABLE_UPDATE'         // 테이블 수정 (REST API 후 브로드캐스트)
  | 'TABLE_DELETE'         // 테이블 삭제 (REST API 후 브로드캐스트)
  | 'COLUMN_CREATE'        // 컬럼 생성 (REST API 후 브로드캐스트)
  | 'COLUMN_UPDATE'        // 컬럼 수정 (REST API 후 브로드캐스트)
  | 'COLUMN_DELETE'        // 컬럼 삭제 (REST API 후 브로드캐스트)
  | 'RELATION_CREATE'      // 관계 생성 (REST API 후 브로드캐스트)
  | 'RELATION_UPDATE'      // 관계 수정 (REST API 후 브로드캐스트)
  | 'RELATION_DELETE';     // 관계 삭제 (REST API 후 브로드캐스트)

// 테이블 이동 이벤트
export interface TableMoveEvent {
  type: 'TABLE_MOVE';
  tableKey: number;
  xPosition: number;
  yPosition: number;
}

// 테이블 이동 완료 이벤트
export interface TableMoveEndEvent {
  type: 'TABLE_MOVE_END';
  tableKey: number;
  xPosition: number;
  yPosition: number;
}

// 컬럼 순서 변경 이벤트
export interface ColumnReorderEvent {
  type: 'COLUMN_REORDER';
  columnKey: number;
  columnOrder: number;
}

// 테이블 락 이벤트
export interface TableLockEvent {
  type: 'TABLE_LOCK';
  tableKey: number;
  userEmail?: string;
  userName?: string;
}

// 테이블 언락 이벤트
export interface TableUnlockEvent {
  type: 'TABLE_UNLOCK';
  tableKey: number;
  userEmail?: string;
}

// 커서 위치 공유 이벤트
export interface CursorMoveEvent {
  type: 'CURSOR_MOVE';
  projectKey: number;
  userEmail?: string;
  userName?: string;
  userColor?: string;
  xPosition: number;
  yPosition: number;
}

// REST API 브로드캐스트 이벤트 (타입만 정의, 구체적 데이터는 백엔드에서)
export interface TableCreateEvent {
  type: 'TABLE_CREATE';
  [key: string]: unknown;
}

export interface TableUpdateEvent {
  type: 'TABLE_UPDATE';
  [key: string]: unknown;
}

export interface TableDeleteEvent {
  type: 'TABLE_DELETE';
  [key: string]: unknown;
}

export interface ColumnCreateEvent {
  type: 'COLUMN_CREATE';
  [key: string]: unknown;
}

export interface ColumnUpdateEvent {
  type: 'COLUMN_UPDATE';
  [key: string]: unknown;
}

export interface ColumnDeleteEvent {
  type: 'COLUMN_DELETE';
  [key: string]: unknown;
}

export interface RelationCreateEvent {
  type: 'RELATION_CREATE';
  [key: string]: unknown;
}

export interface RelationUpdateEvent {
  type: 'RELATION_UPDATE';
  [key: string]: unknown;
}

export interface RelationDeleteEvent {
  type: 'RELATION_DELETE';
  [key: string]: unknown;
}

// 모든 이벤트 타입의 유니온
export type ErdEvent =
  | TableMoveEvent
  | TableMoveEndEvent
  | ColumnReorderEvent
  | TableLockEvent
  | TableUnlockEvent
  | CursorMoveEvent
  | TableCreateEvent
  | TableUpdateEvent
  | TableDeleteEvent
  | ColumnCreateEvent
  | ColumnUpdateEvent
  | ColumnDeleteEvent
  | RelationCreateEvent
  | RelationUpdateEvent
  | RelationDeleteEvent;

// 브로드캐스트 이벤트 (서버에서 받는 형식)
export interface ErdBroadcastEvent {
  projectKey: number;
  userKey: number;
  event: ErdEvent;
}

// WebSocket 응답 형식
export interface WebSocketResponse {
  isSuccess: boolean;
  code: string;
  message: string;
  data: ErdBroadcastEvent;
}

// 락 정보
export interface LockInfo {
  userEmail: string;
  userName: string;
}

// 원격 커서 정보
export interface RemoteCursor {
  userEmail: string;
  userName: string;
  userColor: string;
  xPosition: number;
  yPosition: number;
  element?: HTMLElement;
}

// WebSocket 연결 옵션
export interface WebSocketOptions {
  projectKey: number;
  accessToken: string;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
}

// 이벤트 핸들러 타입
export interface EventHandlers {
  onTableMove?: (event: ErdBroadcastEvent) => void;
  onTableMoveEnd?: (event: ErdBroadcastEvent) => void;
  onColumnReorder?: (event: ErdBroadcastEvent) => void;
  onTableLock?: (event: ErdBroadcastEvent) => void;
  onTableUnlock?: (event: ErdBroadcastEvent) => void;
  onCursorMove?: (event: ErdBroadcastEvent) => void;
  onTableCreate?: (event: ErdBroadcastEvent) => void;
  onTableUpdate?: (event: ErdBroadcastEvent) => void;
  onTableDelete?: (event: ErdBroadcastEvent) => void;
  onColumnCreate?: (event: ErdBroadcastEvent) => void;
  onColumnUpdate?: (event: ErdBroadcastEvent) => void;
  onColumnDelete?: (event: ErdBroadcastEvent) => void;
  onRelationCreate?: (event: ErdBroadcastEvent) => void;
  onRelationUpdate?: (event: ErdBroadcastEvent) => void;
  onRelationDelete?: (event: ErdBroadcastEvent) => void;
}
