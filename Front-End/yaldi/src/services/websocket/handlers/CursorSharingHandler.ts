/**
 * 커서 공유 핸들러
 * 다른 사용자의 커서 위치를 실시간으로 표시
 */

import type { ErdWebSocketClient } from '../ErdWebSocketClient';
import type { ErdBroadcastEvent, CursorMoveEvent, RemoteCursor } from '../types';
import { throttle } from '../../../utils/timing';

export interface CursorSharingOptions {
  currentUserEmail: string;
  canvasElement: HTMLElement;
  throttleDelay?: number; // 기본값: 100ms
  cursorHideDelay?: number; // 기본값: 5000ms (5초)
}

export class CursorSharingHandler {
  private wsClient: ErdWebSocketClient;
  private options: CursorSharingOptions;
  private remoteCursors: Map<string, RemoteCursor> = new Map();
  private cursorTimeouts: Map<string, ReturnType<typeof setTimeout>> = new Map();
  private throttledSendCursor: (x: number, y: number) => void;

  constructor(wsClient: ErdWebSocketClient, options: CursorSharingOptions) {
    this.wsClient = wsClient;
    this.options = {
      throttleDelay: 100,
      cursorHideDelay: 5000,
      ...options,
    };

    // 커서 전송 쓰로틀링 (100ms마다 한 번)
    this.throttledSendCursor = throttle(
      (x: number, y: number) => {
        this.wsClient.sendCursorMove(x, y);
      },
      this.options.throttleDelay!
    );

    // 이벤트 핸들러 등록
    this.wsClient.setEventHandlers({
      onCursorMove: this.handleRemoteCursorMove.bind(this),
    });
  }

  /**
   * 초기화 (캔버스에 이벤트 리스너 추가)
   */
  initialize(): void {
    const canvas = this.options.canvasElement;

    canvas.addEventListener('mousemove', this.handleMouseMove.bind(this));
    canvas.addEventListener('mouseleave', this.handleMouseLeave.bind(this));

    console.log('🖱️ Cursor sharing initialized');
  }

  /**
   * 마우스 이동 핸들러
   */
  private handleMouseMove(event: MouseEvent): void {
    const canvas = this.options.canvasElement;
    const rect = canvas.getBoundingClientRect();

    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    // 쓰로틀링된 전송
    this.throttledSendCursor(x, y);
  }

  /**
   * 마우스 떠남 핸들러
   */
  private handleMouseLeave(): void {
    // 커서가 캔버스를 벗어나면 화면 밖 위치로 전송
    this.wsClient.sendCursorMove(-1000, -1000);
  }

  /**
   * 원격 커서 이동 처리
   */
  private handleRemoteCursorMove(event: ErdBroadcastEvent): void {
    const cursorEvent = event.event as CursorMoveEvent;

    // 자기 커서는 무시
    if (cursorEvent.userEmail === this.options.currentUserEmail) {
      return;
    }

    const userEmail = cursorEvent.userEmail!;

    // 화면 밖이면 커서 숨김
    if (cursorEvent.xPosition < 0 || cursorEvent.yPosition < 0) {
      this.hideCursor(userEmail);
      return;
    }

    // 원격 커서 업데이트
    this.updateRemoteCursor({
      userEmail,
      userName: cursorEvent.userName!,
      userColor: cursorEvent.userColor!,
      xPosition: cursorEvent.xPosition,
      yPosition: cursorEvent.yPosition,
    });
  }

  /**
   * 원격 커서 업데이트
   */
  private updateRemoteCursor(cursor: Omit<RemoteCursor, 'element'>): void {
    let remoteCursor = this.remoteCursors.get(cursor.userEmail);

    // 커서가 없으면 생성
    if (!remoteCursor) {
      remoteCursor = {
        ...cursor,
        element: this.createCursorElement(cursor.userName, cursor.userColor),
      };
      this.remoteCursors.set(cursor.userEmail, remoteCursor);
      this.options.canvasElement.appendChild(remoteCursor.element!);
    }

    // 커서 위치 업데이트
    remoteCursor.xPosition = cursor.xPosition;
    remoteCursor.yPosition = cursor.yPosition;

    if (remoteCursor.element) {
      remoteCursor.element.style.left = `${cursor.xPosition}px`;
      remoteCursor.element.style.top = `${cursor.yPosition}px`;
      remoteCursor.element.style.display = 'block';
    }

    // 자동 숨김 타이머 리셋
    this.resetCursorTimeout(cursor.userEmail);
  }

  /**
   * 커서 엘리먼트 생성
   */
  private createCursorElement(userName: string, color: string): HTMLElement {
    const cursor = document.createElement('div');
    cursor.className = 'remote-cursor';
    cursor.style.position = 'absolute';
    cursor.style.pointerEvents = 'none';
    cursor.style.zIndex = '9999';
    cursor.style.transition = 'left 0.1s, top 0.1s';

    cursor.innerHTML = `
      <svg width="24" height="24" viewBox="0 0 24 24" style="filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));">
        <path fill="${color}" d="M3 3l7 13 3-7 7-3z"/>
      </svg>
      <span style="
        position: absolute;
        top: 24px;
        left: 12px;
        background: ${color};
        color: white;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 12px;
        white-space: nowrap;
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
      ">${userName}</span>
    `;

    return cursor;
  }

  /**
   * 커서 자동 숨김 타이머 리셋
   */
  private resetCursorTimeout(userEmail: string): void {
    // 기존 타이머 취소
    const existingTimeout = this.cursorTimeouts.get(userEmail);
    if (existingTimeout) {
      clearTimeout(existingTimeout);
    }

    // 새 타이머 설정
    const timeout = setTimeout(() => {
      this.hideCursor(userEmail);
    }, this.options.cursorHideDelay!);

    this.cursorTimeouts.set(userEmail, timeout);
  }

  /**
   * 커서 숨김
   */
  private hideCursor(userEmail: string): void {
    const remoteCursor = this.remoteCursors.get(userEmail);
    if (remoteCursor?.element) {
      remoteCursor.element.style.display = 'none';
    }
  }

  /**
   * 특정 사용자 커서 제거
   */
  removeCursor(userEmail: string): void {
    const remoteCursor = this.remoteCursors.get(userEmail);
    if (remoteCursor?.element) {
      remoteCursor.element.remove();
    }

    const timeout = this.cursorTimeouts.get(userEmail);
    if (timeout) {
      clearTimeout(timeout);
    }

    this.remoteCursors.delete(userEmail);
    this.cursorTimeouts.delete(userEmail);
  }

  /**
   * 모든 커서 제거
   */
  removeAllCursors(): void {
    this.remoteCursors.forEach((cursor) => {
      cursor.element?.remove();
    });

    this.cursorTimeouts.forEach((timeout) => {
      clearTimeout(timeout);
    });

    this.remoteCursors.clear();
    this.cursorTimeouts.clear();
  }

  /**
   * 정리 (컴포넌트 언마운트 시 호출)
   */
  cleanup(): void {
    this.removeAllCursors();
    console.log('🖱️ Cursor sharing cleaned up');
  }
}
