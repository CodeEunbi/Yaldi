/**
 * 테이블 락 핸들러
 * 테이블 편집 시 다른 사용자의 동시 편집 방지
 */

import type { ErdWebSocketClient } from '../ErdWebSocketClient';
import type {
  ErdBroadcastEvent,
  TableLockEvent,
  TableUnlockEvent,
  LockInfo,
} from '../types';

export interface TableLockOptions {
  currentUserEmail: string;
  onLockAcquired?: (tableKey: number) => void;
  onLockReleased?: (tableKey: number) => void;
  onLockFailed?: (tableKey: number, lockInfo: LockInfo) => void;
  onRemoteLock?: (tableKey: number, lockInfo: LockInfo) => void;
  onRemoteUnlock?: (tableKey: number) => void;
}

export class TableLockHandler {
  private wsClient: ErdWebSocketClient;
  private options: TableLockOptions;
  private lockedTables: Map<number, LockInfo> = new Map(); // tableKey → LockInfo
  private myLocks: Set<number> = new Set(); // 내가 보유한 락

  constructor(wsClient: ErdWebSocketClient, options: TableLockOptions) {
    this.wsClient = wsClient;
    this.options = options;

    // 이벤트 핸들러 등록
    this.wsClient.setEventHandlers({
      onTableLock: this.handleRemoteTableLock.bind(this),
      onTableUnlock: this.handleRemoteTableUnlock.bind(this),
    });
  }

  /**
   * 테이블 락 요청
   * @returns 락 획득 성공 여부
   */
  requestLock(tableKey: number): boolean {
    // 이미 다른 사람이 락 보유 중이면 실패
    const existingLock = this.lockedTables.get(tableKey);
    if (existingLock && existingLock.userEmail !== this.options.currentUserEmail) {
      console.warn(
        `❌ Table ${tableKey} is locked by ${existingLock.userName}`
      );
      this.options.onLockFailed?.(tableKey, existingLock);
      return false;
    }

    // 이미 내가 보유한 락이면 성공
    if (this.myLocks.has(tableKey)) {
      console.log(`✅ Already locked by me: ${tableKey}`);
      return true;
    }

    // 락 요청 전송
    this.wsClient.sendTableLock(tableKey);

    console.log(`🔒 Lock requested: ${tableKey}`);
    return true;
  }

  /**
   * 테이블 락 해제
   */
  releaseLock(tableKey: number): void {
    if (!this.myLocks.has(tableKey)) {
      console.warn(`⚠️ Not locked by me: ${tableKey}`);
      return;
    }

    // 락 해제 전송
    this.wsClient.sendTableUnlock(tableKey);

    // 로컬 상태 업데이트
    this.myLocks.delete(tableKey);
    this.lockedTables.delete(tableKey);

    console.log(`🔓 Lock released: ${tableKey}`);
    this.options.onLockReleased?.(tableKey);
  }

  /**
   * 내가 보유한 모든 락 해제
   */
  releaseAllLocks(): void {
    this.myLocks.forEach((tableKey) => {
      this.releaseLock(tableKey);
    });
  }

  /**
   * 원격 테이블 락 처리
   */
  private handleRemoteTableLock(event: ErdBroadcastEvent): void {
    const lockEvent = event.event as TableLockEvent;
    const { tableKey, userEmail, userName } = lockEvent;

    if (!userEmail || !userName) {
      console.error('Invalid lock event: missing user info');
      return;
    }

    const lockInfo: LockInfo = { userEmail, userName };

    // 락 정보 저장
    this.lockedTables.set(tableKey, lockInfo);

    // 내가 요청한 락이면
    if (userEmail === this.options.currentUserEmail) {
      this.myLocks.add(tableKey);
      console.log(`✅ Lock acquired: ${tableKey}`);
      this.options.onLockAcquired?.(tableKey);
    } else {
      // 다른 사용자의 락이면
      console.log(`📥 Remote lock: ${tableKey} by ${userName}`);
      this.options.onRemoteLock?.(tableKey, lockInfo);
      this.showLockIndicator(tableKey, userName);
      this.disableTableEditing(tableKey);
    }
  }

  /**
   * 원격 테이블 언락 처리
   */
  private handleRemoteTableUnlock(event: ErdBroadcastEvent): void {
    const unlockEvent = event.event as TableUnlockEvent;
    const { tableKey, userEmail } = unlockEvent;

    if (!userEmail) {
      console.error('Invalid unlock event: missing user email');
      return;
    }

    // 락 정보 삭제
    this.lockedTables.delete(tableKey);

    // 내가 해제한 락이면
    if (userEmail === this.options.currentUserEmail) {
      this.myLocks.delete(tableKey);
      console.log(`✅ Lock released: ${tableKey}`);
    } else {
      // 다른 사용자의 락 해제면
      console.log(`📥 Remote unlock: ${tableKey}`);
      this.options.onRemoteUnlock?.(tableKey);
      this.hideLockIndicator(tableKey);
      this.enableTableEditing(tableKey);
    }
  }

  /**
   * 락 인디케이터 표시
   */
  private showLockIndicator(tableKey: number, userName: string): void {
    const table = document.querySelector(`[data-table-key="${tableKey}"]`);
    if (!table) return;

    table.classList.add('locked-by-other');
    table.setAttribute('data-locked-by', userName);

    // 락 인디케이터 생성
    const existingIndicator = table.querySelector('.lock-indicator');
    if (existingIndicator) {
      existingIndicator.remove();
    }

    const indicator = document.createElement('div');
    indicator.className = 'lock-indicator';
    indicator.style.cssText = `
      position: absolute;
      top: -8px;
      right: -8px;
      background: #ff6b6b;
      color: white;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba(255, 107, 107, 0.3);
      z-index: 10;
      pointer-events: none;
    `;
    indicator.textContent = `${userName} 편집 중`;
    table.appendChild(indicator);
  }

  /**
   * 락 인디케이터 숨김
   */
  private hideLockIndicator(tableKey: number): void {
    const table = document.querySelector(`[data-table-key="${tableKey}"]`);
    if (!table) return;

    table.classList.remove('locked-by-other');
    table.removeAttribute('data-locked-by');

    const indicator = table.querySelector('.lock-indicator');
    if (indicator) {
      indicator.remove();
    }
  }

  /**
   * 테이블 편집 비활성화
   */
  private disableTableEditing(tableKey: number): void {
    const table = document.querySelector(`[data-table-key="${tableKey}"]`);
    if (!table) return;

    // 편집 버튼 비활성화
    const editButtons = table.querySelectorAll('button, input, textarea');
    editButtons.forEach((element) => {
      (element as HTMLElement).style.pointerEvents = 'none';
      (element as HTMLElement).style.opacity = '0.5';
    });

    // 시각적 피드백
    (table as HTMLElement).style.opacity = '0.7';
  }

  /**
   * 테이블 편집 활성화
   */
  private enableTableEditing(tableKey: number): void {
    const table = document.querySelector(`[data-table-key="${tableKey}"]`);
    if (!table) return;

    // 편집 버튼 활성화
    const editButtons = table.querySelectorAll('button, input, textarea');
    editButtons.forEach((element) => {
      (element as HTMLElement).style.pointerEvents = '';
      (element as HTMLElement).style.opacity = '';
    });

    // 시각적 피드백 제거
    (table as HTMLElement).style.opacity = '';
  }

  /**
   * 테이블이 락 상태인지 확인
   */
  isLocked(tableKey: number): boolean {
    return this.lockedTables.has(tableKey);
  }

  /**
   * 내가 락을 보유하고 있는지 확인
   */
  isLockedByMe(tableKey: number): boolean {
    return this.myLocks.has(tableKey);
  }

  /**
   * 락 소유자 정보 조회
   */
  getLockInfo(tableKey: number): LockInfo | null {
    return this.lockedTables.get(tableKey) || null;
  }

  /**
   * 정리 (컴포넌트 언마운트 시 호출)
   */
  cleanup(): void {
    this.releaseAllLocks();
    console.log('🔒 Table lock handler cleaned up');
  }
}
