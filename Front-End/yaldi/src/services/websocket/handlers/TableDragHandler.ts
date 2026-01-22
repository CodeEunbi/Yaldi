/**
 * 테이블 드래그 핸들러
 * 테이블 이동 시 실시간 동기화 처리
 */

import type { ErdWebSocketClient } from '../ErdWebSocketClient';
import type { ErdBroadcastEvent, TableMoveEvent } from '../types';

export interface Position {
  x: number;
  y: number;
}

export interface TableDragOptions {
  currentUserKey: number;
  onLocalUpdate?: (tableKey: number, position: Position) => void;
  onRemoteUpdate?: (tableKey: number, position: Position, userKey: number) => void;
}

export class TableDragHandler {
  private wsClient: ErdWebSocketClient;
  private options: TableDragOptions;
  private isDragging = false;
  private currentTableKey: number | null = null;

  constructor(wsClient: ErdWebSocketClient, options: TableDragOptions) {
    this.wsClient = wsClient;
    this.options = options;

    // 이벤트 핸들러 등록
    this.wsClient.setEventHandlers({
      onTableMove: this.handleRemoteTableMove.bind(this),
      onTableMoveEnd: this.handleRemoteTableMoveEnd.bind(this),
    });
  }

  /**
   * 드래그 시작
   */
  onDragStart(tableKey: number): void {
    this.isDragging = true;
    this.currentTableKey = tableKey;
    console.log('🎯 Drag started:', tableKey);
  }

  /**
   * 드래그 중 (로컬 업데이트 + 서버 전송)
   */
  onDrag(position: Position): void {
    if (!this.isDragging || this.currentTableKey === null) {
      return;
    }

    // 1. 로컬 화면 즉시 업데이트 (Optimistic Update)
    this.options.onLocalUpdate?.(this.currentTableKey, position);

    // 2. 서버로 전송 (다른 사용자에게 브로드캐스트)
    this.wsClient.sendTableMove(
      this.currentTableKey,
      position.x,
      position.y
    );
  }

  /**
   * 드래그 완료 (최종 위치 DB 저장)
   */
  onDragEnd(position: Position): void {
    if (!this.isDragging || this.currentTableKey === null) {
      return;
    }

    console.log('✅ Drag ended:', this.currentTableKey, position);

    // 최종 위치 DB 저장
    this.wsClient.sendTableMoveEnd(
      this.currentTableKey,
      position.x,
      position.y
    );

    this.isDragging = false;
    this.currentTableKey = null;
  }

  /**
   * 원격 테이블 이동 처리 (다른 사용자의 드래그)
   */
  private handleRemoteTableMove(event: ErdBroadcastEvent): void {
    // 자기 이벤트는 무시 (이미 로컬 업데이트 완료)
    if (event.userKey === this.options.currentUserKey) {
      return;
    }

    const moveEvent = event.event as TableMoveEvent;
    const position: Position = {
      x: moveEvent.xPosition,
      y: moveEvent.yPosition,
    };

    console.log('📥 Remote table move:', moveEvent.tableKey, position);

    // 원격 업데이트 콜백 호출
    this.options.onRemoteUpdate?.(
      moveEvent.tableKey,
      position,
      event.userKey
    );
  }

  /**
   * 원격 테이블 이동 완료 처리
   */
  private handleRemoteTableMoveEnd(event: ErdBroadcastEvent): void {
    // 이미 TABLE_MOVE로 위치가 업데이트되었으므로 별도 처리 불필요
    // 필요시 최종 위치 확정 로직 추가 가능
    console.log('📥 Remote table move end:', event);
  }

  /**
   * 현재 드래그 상태 확인
   */
  getIsDragging(): boolean {
    return this.isDragging;
  }

  /**
   * 현재 드래그 중인 테이블 키
   */
  getCurrentTableKey(): number | null {
    return this.currentTableKey;
  }
}
