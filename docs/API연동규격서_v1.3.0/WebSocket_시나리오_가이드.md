# WebSocket 이벤트 시나리오 가이드

## 📋 목차
1. [연결 및 인증](#1-연결-및-인증)
2. [실시간 협업 시나리오](#2-실시간-협업-시나리오)
3. [테이블 작업 시나리오](#3-테이블-작업-시나리오)
4. [컬럼 작업 시나리오](#4-컬럼-작업-시나리오)
5. [관계 작업 시나리오](#5-관계-작업-시나리오)
6. [에러 처리](#6-에러-처리)
7. [이벤트 타입 분류](#7-이벤트-타입-분류)

---

## 1. 연결 및 인증

### 시나리오 1-1: 프로젝트 접속

```javascript
// 1. 프론트: WebSocket 연결 시작
const socket = new SockJS('https://api.yaldi.kr/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
  console.log('연결 성공:', frame);

  // 2. 프론트: 프로젝트 토픽 구독
  stompClient.subscribe('/topic/project/123', (message) => {
    const event = JSON.parse(message.body);
    handleEvent(event);
  });
});
```

**백엔드 처리:**
1. 쿠키에서 JWT 토큰 검증
2. 사용자의 프로젝트 접근 권한 확인 (OWNER/EDITOR/VIEWER)
3. WebSocket 세션 생성 및 프로젝트 토픽에 연결

**프론트 수신:**
```javascript
{
  "type": "CONNECTION_ESTABLISHED",
  "data": {
    "sessionId": "sess_abc123",
    "projectKey": 123,
    "userKey": 456,
    "role": "EDITOR",
    "serverTime": "2025-01-14T12:00:00Z"
  }
}
```

### 시나리오 1-2: 멤버 입장/퇴장 알림

**다른 사용자가 접속했을 때:**
```javascript
// 프론트: 모든 접속자에게 브로드캐스트
{
  "type": "MEMBER_JOIN",
  "data": {
    "userKey": 789,
    "userEmail": "user3@example.com",
    "userName": "김철수",
    "role": "EDITOR",
    "joinedAt": "2025-01-14T12:05:00Z"
  }
}

// 프론트 처리
if (event.type === 'MEMBER_JOIN') {
  showNotification(`${event.data.userName}님이 입장했습니다.`);
  updateOnlineUsersList(event.data);
}
```

**사용자가 퇴장했을 때:**
```javascript
{
  "type": "MEMBER_LEAVE",
  "data": {
    "userKey": 789,
    "userEmail": "user3@example.com",
    "userName": "김철수",
    "leftAt": "2025-01-14T12:10:00Z",
    "reason": "DISCONNECTED"  // DISCONNECTED | TIMEOUT | KICKED
  }
}
```

---

## 2. 실시간 협업 시나리오

### 시나리오 2-1: 실시간 커서 공유

**프론트: 마우스 이동 시 (Throttling 50-100ms)**
```javascript
let lastCursorSent = 0;
const THROTTLE_MS = 50;

canvas.addEventListener('mousemove', (e) => {
  const now = Date.now();
  if (now - lastCursorSent < THROTTLE_MS) return;

  // 프론트 → 백: 커서 위치 전송
  stompClient.send('/pub/erd/cursor', {}, JSON.stringify({
    projectKey: 123,
    xPosition: e.clientX,
    yPosition: e.clientY
  }));

  lastCursorSent = now;
});
```

**백엔드 처리:**
1. 발신자의 userKey, userName, userColor 추가
2. **즉시** 모든 구독자에게 브로드캐스트 (DB/Redis 저장 안함, Kafka 안씀)
3. 평균 지연시간: **60ms**

**프론트 수신: 다른 사용자의 커서 표시**
```javascript
if (event.type === 'CursorPosEvent') {
  const cursor = event.event;
  canvas.showCursor({
    user: cursor.userName,
    color: cursor.userColor,  // 이메일 해시 기반 자동 생성
    x: cursor.xPosition,
    y: cursor.yPosition
  });
}
```

**이벤트 타입: D (Direct Broadcast)**
- ✅ 완전 휘발성 (DB/Redis 저장 없음)
- ✅ Kafka 미사용
- ✅ 초당 최대 30회 전송 권장

---

## 3. 테이블 작업 시나리오

### 시나리오 3-1: 테이블 생성

**프론트: REST API 호출**
```javascript
// 1. 프론트 → 백: REST API로 테이블 생성
const response = await fetch('/api/v1/erd/projects/123/tables', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',  // 쿠키 자동 전송
  body: JSON.stringify({
    logicalName: '회원',
    physicalName: 'members',
    xPosition: 100.0,
    yPosition: 200.0,
    colorHex: 'FF6B6B'
  })
});
```

**백엔드 처리:**
1. JWT 토큰 검증 (쿠키)
2. 권한 확인 (EDITOR 이상만 생성 가능)
3. PostgreSQL DB에 저장
4. Kafka에 이벤트 발행
5. 모든 구독자에게 브로드캐스트 (2초 배치 압축)

**프론트 수신: WebSocket 이벤트**
```javascript
// 2. 백 → 프론트: WebSocket으로 자동 브로드캐스트
stompClient.subscribe('/topic/project/123', (message) => {
  const event = JSON.parse(message.body);

  if (event.type === 'TableNewEvent') {
    const table = event.event;

    // 캔버스에 새 테이블 추가
    canvas.addTable({
      id: table.tableKey,
      name: table.logicalName,
      physicalName: table.physicalName,
      x: table.xPosition,
      y: table.yPosition,
      color: '#' + table.colorHex
    });
  }
});
```

**이벤트 타입: A (REST + Kafka)**
- ✅ DB 저장: PostgreSQL
- ✅ Kafka 브로드캐스트
- ✅ Batch Compression 적용 (2초)

### 시나리오 3-2: 테이블 드래그 (실시간 이동)

**프론트: 드래그 중**
```javascript
let isDragging = false;

onTableDragStart = (tableKey) => {
  isDragging = true;
};

onTableDrag = (tableKey, x, y) => {
  if (!isDragging) return;

  // 프론트 → 백: 드래그 중 위치 전송
  stompClient.send('/pub/erd/table/move', {}, JSON.stringify({
    tableKey: tableKey,
    xPosition: x,
    yPosition: y
  }));
};
```

**백엔드 처리:**
1. **DB 저장 안함** (휘발성)
2. **즉시** 모든 구독자에게 브로드캐스트
3. Kafka 미사용
4. 평균 지연시간: **60ms**

**프론트 수신: 다른 사용자가 드래그하는 테이블 실시간 이동**
```javascript
if (event.type === 'TableMoveEvent') {
  const { tableKey, xPosition, yPosition } = event.event;
  canvas.moveTable(tableKey, xPosition, yPosition);
}
```

**이벤트 타입: A-1 (Direct Broadcast)**
- ✅ 휘발성 (DB 저장 안함)
- ✅ Kafka 미사용
- ✅ 즉시 브로드캐스트

### 시나리오 3-3: 테이블 드래그 완료

**프론트: 드래그 종료**
```javascript
onTableDragEnd = (tableKey, x, y) => {
  isDragging = false;

  // 프론트 → 백: 최종 위치 저장
  stompClient.send('/pub/erd/table/move/end', {}, JSON.stringify({
    tableKey: tableKey,
    xPosition: x,
    yPosition: y
  }));
};
```

**백엔드 처리:**
1. PostgreSQL DB에 **최종 위치만 저장**
2. **브로드캐스트 안함** (응답 없음)
3. Kafka 미사용

**이벤트 타입: A-2 (DB 저장만)**
- ✅ DB 저장: PostgreSQL
- ❌ 브로드캐스트 없음

### 시나리오 3-4: 테이블 편집 잠금

**프론트: 테이블 더블클릭 시 잠금 요청**
```javascript
onTableDoubleClick = (tableKey) => {
  // 프론트 → 백: 잠금 요청
  stompClient.send('/pub/erd/table/lock', {}, JSON.stringify({
    tableKey: tableKey
  }));
};
```

**백엔드 처리:**
1. Redis에 잠금 저장 (TTL 30초)
2. 이미 잠금이 있으면 **ERROR 이벤트 전송**
3. 성공 시 Kafka 브로드캐스트
4. Heartbeat 10초마다 자동 갱신

**프론트 수신: 잠금 성공**
```javascript
if (event.type === 'TableLockEvent') {
  const { tableKey, userName } = event.event;
  canvas.lockTable(tableKey, userName);
  showToast(`${userName}님이 편집 중입니다`);
}
```

**프론트 수신: 잠금 실패 (다른 사용자가 이미 편집 중)**
```javascript
if (event.type === 'ERROR') {
  const { code, message, details } = event.data;

  if (code === 'OPERATION_FAILED') {
    showError(`${details.lockedBy}님이 이미 편집 중입니다.`);
  }
}
```

**이벤트 타입: C (WebSocket + Redis + Kafka)**
- ✅ Redis 잠금 (TTL 30초)
- ✅ Kafka 브로드캐스트
- ✅ 연결 해제 시 자동 해제

### 시나리오 3-5: 테이블 편집 완료 (잠금 해제)

**프론트: 편집 완료**
```javascript
onTableEditComplete = (tableKey) => {
  // 프론트 → 백: 잠금 해제
  stompClient.send('/pub/erd/table/unlock', {}, JSON.stringify({
    tableKey: tableKey
  }));
};
```

**백엔드 처리:**
1. Redis 잠금 삭제
2. Kafka 브로드캐스트
3. **WebSocket 연결 해제 시 자동으로 모든 잠금 해제됨**

**프론트 수신:**
```javascript
if (event.type === 'TableUnlockEvent') {
  const { tableKey } = event.event;
  canvas.unlockTable(tableKey);
}
```

### 시나리오 3-6: 테이블 수정 (논리명/물리명/색상)

**프론트: REST API 호출**
```javascript
// 프론트 → 백: PATCH 요청
await fetch(`/api/v1/erd/tables/${tableKey}`, {
  method: 'PATCH',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    logicalName: '사용자',
    physicalName: 'users',
    colorHex: '4ECDC4'
  })
});
```

**백엔드 처리:**
1. PostgreSQL DB 업데이트
2. **변경된 필드별로 개별 이벤트 발행** (Kafka)
   - 논리명 변경 → `TableLnameEvent`
   - 물리명 변경 → `TablePnameEvent`
   - 색상 변경 → `TableColorEvent`

**프론트 수신:**
```javascript
// 백 → 프론트: 3개의 개별 이벤트
if (event.type === 'TableLnameEvent') {
  canvas.updateTableName(event.event.tableKey, event.event.logicalName);
}

if (event.type === 'TablePnameEvent') {
  canvas.updateTablePhysicalName(event.event.tableKey, event.event.physicalName);
}

if (event.type === 'TableColorEvent') {
  canvas.updateTableColor(event.event.tableKey, event.event.colorHex);
}
```

### 시나리오 3-7: 테이블 삭제

**프론트: REST API 호출**
```javascript
// 프론트 → 백: DELETE 요청
await fetch(`/api/v1/erd/tables/${tableKey}`, {
  method: 'DELETE',
  credentials: 'include'
});
```

**백엔드 처리:**
1. PostgreSQL에서 Soft Delete (deletedAt 컬럼 업데이트)
2. 관련 **컬럼과 관계도 함께 삭제**
3. Kafka 브로드캐스트

**프론트 수신:**
```javascript
if (event.type === 'TableDelEvent') {
  const { tableKey } = event.event;
  canvas.removeTable(tableKey);
}
```

---

## 4. 컬럼 작업 시나리오

### 시나리오 4-1: 컬럼 생성

**프론트: REST API 호출**
```javascript
// 프론트 → 백: POST 요청
await fetch(`/api/v1/erd/tables/${tableKey}/columns`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    logicalName: '회원 아이디',
    physicalName: 'member_id',
    dataType: 'BIGINT',
    isPrimaryKey: true,
    isNullable: false,
    isUnique: true,
    isIncremental: true
  })
});
```

**백엔드 처리:**
1. PostgreSQL DB 저장
2. Kafka 브로드캐스트

**프론트 수신:**
```javascript
if (event.type === 'ColumnNewEvent') {
  const column = event.event;
  canvas.addColumn(column.tableKey, {
    id: column.columnKey,
    name: column.logicalName,
    type: column.dataType,
    isPK: column.isPrimaryKey,
    order: column.columnOrder
  });
}
```

### 시나리오 4-2: 컬럼 순서 변경 (드래그 앤 드롭)

**프론트: WebSocket 발행**
```javascript
// 프론트 → 백: 컬럼 순서 변경
onColumnReorder = (columnKey, newOrder) => {
  stompClient.send('/pub/erd/column/reorder', {}, JSON.stringify({
    columnKey: columnKey,
    columnOrder: newOrder
  }));
};
```

**백엔드 처리:**
1. PostgreSQL DB 업데이트
2. Kafka 브로드캐스트

**프론트 수신:**
```javascript
if (event.type === 'ColumnOrderEvent') {
  const { columnKey, columnOrder } = event.event;
  canvas.reorderColumn(columnKey, columnOrder);
}
```

**이벤트 타입: B (WebSocket + DB + Kafka)**
- ✅ DB 저장: PostgreSQL
- ✅ Kafka 브로드캐스트

### 시나리오 4-3: 컬럼 수정

**프론트: REST API 호출**
```javascript
// 프론트 → 백: PATCH 요청
await fetch(`/api/v1/erd/columns/${columnKey}`, {
  method: 'PATCH',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    logicalName: '사용자 이메일',
    dataType: 'VARCHAR',
    dataDetail: ['255']
  })
});
```

**백엔드 처리:**
1. PostgreSQL DB 업데이트
2. Kafka 브로드캐스트

**프론트 수신:**
```javascript
if (event.type === 'ColumnUpdateEvent') {
  const column = event.event;
  canvas.updateColumn(column.columnKey, {
    name: column.logicalName,
    type: column.dataType,
    detail: column.dataDetail
  });
}
```

### 시나리오 4-4: 컬럼 삭제

**프론트: REST API 호출**
```javascript
await fetch(`/api/v1/erd/columns/${columnKey}`, {
  method: 'DELETE',
  credentials: 'include'
});
```

**백엔드 처리:**
1. PostgreSQL Soft Delete
2. PK/FK인 경우 **관련 관계도 함께 삭제**
3. Kafka 브로드캐스트

**프론트 수신:**
```javascript
if (event.type === 'ColumnDelEvent') {
  const { columnKey } = event.event;
  canvas.removeColumn(columnKey);
}
```

---

## 5. 관계 작업 시나리오

### 시나리오 5-1: 관계 생성

**프론트: REST API 호출**
```javascript
await fetch(`/api/v1/erd/projects/${projectKey}/relations`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    fromTableKey: 10,
    toTableKey: 20,
    relationType: 'ONE_TO_MANY',
    constraintName: 'fk_order_member',
    onDeleteAction: 'CASCADE',
    onUpdateAction: 'NO_ACTION'
  })
});
```

**백엔드 처리:**
1. PostgreSQL DB 저장
2. Kafka 브로드캐스트

**프론트 수신:**
```javascript
if (event.type === 'RelationNewEvent') {
  const relation = event.event;
  canvas.addRelation({
    id: relation.relationKey,
    from: relation.fromTableKey,
    to: relation.toTableKey,
    type: relation.relationType,
    onDelete: relation.onDeleteAction
  });
}
```

**관계 타입:**
- `ONE_TO_ONE`: 1:1
- `ONE_TO_MANY`: 1:N
- `MANY_TO_ONE`: N:1
- `MANY_TO_MANY`: N:M

### 시나리오 5-2: 관계 수정

**프론트: REST API 호출**
```javascript
await fetch(`/api/v1/erd/relations/${relationKey}`, {
  method: 'PATCH',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    relationType: 'MANY_TO_ONE',
    onDeleteAction: 'SET_NULL'
  })
});
```

**프론트 수신:**
```javascript
if (event.type === 'RelationUpdateEvent') {
  const relation = event.event;
  canvas.updateRelation(relation.relationKey, {
    type: relation.relationType,
    onDelete: relation.onDeleteAction
  });
}
```

### 시나리오 5-3: 관계 삭제

**프론트: REST API 호출**
```javascript
await fetch(`/api/v1/erd/relations/${relationKey}`, {
  method: 'DELETE',
  credentials: 'include'
});
```

**프론트 수신:**
```javascript
if (event.type === 'RelationDelEvent') {
  const { relationKey } = event.event;
  canvas.removeRelation(relationKey);
}
```

---

## 6. 에러 처리

### 시나리오 6-1: 권한 부족 (Viewer가 편집 시도)

**백 → 프론트:**
```javascript
{
  "type": "ERROR",
  "data": {
    "code": "FORBIDDEN",
    "message": "편집 권한이 없습니다. Viewer는 읽기 전용입니다.",
    "details": {
      "role": "VIEWER"
    }
  }
}
```

### 시나리오 6-2: 테이블 잠금 실패 (다른 사용자가 편집 중)

**백 → 프론트:**
```javascript
{
  "type": "ERROR",
  "data": {
    "code": "OPERATION_FAILED",
    "message": "테이블 잠금 획득 실패",
    "details": {
      "reason": "이미 다른 사용자가 편집중입니다.",
      "tableKey": 789,
      "lockedBy": "user@example.com"
    }
  }
}
```

### 시나리오 6-3: Rate Limit 초과

**백 → 프론트:**
```javascript
{
  "type": "ERROR",
  "data": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "초당 최대 10개 메시지를 초과했습니다.",
    "details": {
      "limit": 10,
      "current": 15
    }
  }
}
```

**프론트 처리:**
```javascript
stompClient.subscribe('/topic/project/123', (message) => {
  const event = JSON.parse(message.body);

  if (event.type === 'ERROR') {
    const { code, message, details } = event.data;

    switch (code) {
      case 'RATE_LIMIT_EXCEEDED':
        showNotification('작업이 너무 빠릅니다. 잠시 후 다시 시도하세요.');
        break;
      case 'FORBIDDEN':
        showError('편집 권한이 없습니다.');
        break;
      case 'OPERATION_FAILED':
        showError(message);
        break;
      default:
        console.error('Unexpected error:', message);
    }
  }
});
```

**에러 코드 목록:**
- `UNAUTHORIZED`: 인증 실패
- `FORBIDDEN`: 권한 없음 (Viewer가 편집 시도)
- `VALIDATION_ERROR`: 유효성 검증 실패
- `OPERATION_FAILED`: 작업 실패 (잠금 실패 등)
- `CONFLICT`: 충돌 (동시 수정 등)
- `RATE_LIMIT_EXCEEDED`: 속도 제한 초과 (초당 10개)
- `MESSAGE_TOO_LARGE`: 메시지 크기 초과 (1MB)
- `INTERNAL_ERROR`: 서버 오류

---

## 7. 이벤트 타입 분류

### A 타입: REST + Kafka (11개)
**특징:**
- REST API 호출 시 자동 브로드캐스트
- DB 저장 → Kafka 발행 → 모든 구독자에게 전송
- Batch Compression 적용 (2초 간격)

**이벤트 목록:**
1. `TableNewEvent` - 테이블 생성
2. `TableLnameEvent` - 테이블 논리명 변경
3. `TablePnameEvent` - 테이블 물리명 변경
4. `TableColorEvent` - 테이블 색상 변경
5. `TableDelEvent` - 테이블 삭제
6. `ColumnNewEvent` - 컬럼 생성
7. `ColumnUpdateEvent` - 컬럼 수정
8. `ColumnDelEvent` - 컬럼 삭제
9. `RelationNewEvent` - 관계 생성
10. `RelationUpdateEvent` - 관계 수정
11. `RelationDelEvent` - 관계 삭제

### A-1 타입: Direct Broadcast (1개)
**특징:**
- **휘발성** (DB 저장 안함)
- Kafka 미사용
- 즉시 브로드캐스트
- 평균 지연시간: 60ms

**이벤트:**
1. `TableMoveEvent` - 테이블 드래그 중 (실시간 이동)

### A-2 타입: DB 저장만 (1개)
**특징:**
- DB에만 저장
- 브로드캐스트 없음 (응답 없음)

**이벤트:**
1. `TableMoveEnd` - 테이블 드래그 완료 (최종 위치 저장)

### B 타입: WebSocket + DB + Kafka (1개)
**특징:**
- WebSocket으로 발행
- DB 업데이트 후 Kafka 브로드캐스트
- Batch Compression 적용

**이벤트:**
1. `ColumnOrderEvent` - 컬럼 순서 변경

### C 타입: WebSocket + Redis + Kafka (2개)
**특징:**
- WebSocket으로 발행
- Redis 잠금 관리 (TTL 30초)
- Kafka 브로드캐스트
- 연결 해제 시 자동 해제

**이벤트:**
1. `TableLockEvent` - 테이블 편집 잠금
2. `TableUnlockEvent` - 테이블 편집 잠금 해제

### D 타입: Direct Broadcast (완전 휘발성) (1개)
**특징:**
- **완전 휘발성** (DB/Redis 저장 없음)
- Kafka 미사용
- WebSocket으로 즉시 브로드캐스트
- 평균 지연시간: 60ms
- 초당 최대 30회 전송 권장

**이벤트:**
1. `CursorPosEvent` - 커서 위치 공유

### 자동 이벤트 (3개)
**특징:**
- 서버가 자동 발행
- 사용자 연결/해제 시 자동 처리

**이벤트:**
1. `CONNECTION_ESTABLISHED` - WebSocket 연결 성공
2. `MEMBER_JOIN` - 멤버 입장
3. `MEMBER_LEAVE` - 멤버 퇴장

### 에러 이벤트 (1개)
**특징:**
- 작업 실패 시 발행
- 8가지 에러 코드

**이벤트:**
1. `ERROR` - 에러 메시지

---

## 8. 보안 및 제약사항

### 8-1. 권한 체계
- **OWNER**: 모든 권한 (삭제, 멤버 관리 포함)
- **EDITOR**: 읽기/쓰기 (ERD 편집, 버전 생성)
- **VIEWER**: 읽기 전용 (구독만 가능, 발행 불가)

### 8-2. Rate Limiting
- **초당 최대 10개 메시지**
- 초과 시 `RATE_LIMIT_EXCEEDED` 에러

```javascript
class RateLimiter {
  constructor(maxPerSecond = 10) {
    this.max = maxPerSecond;
    this.queue = [];
  }

  send(message) {
    const now = Date.now();
    this.queue = this.queue.filter(t => now - t < 1000);

    if (this.queue.length >= this.max) {
      console.warn('Rate limit exceeded');
      return false;
    }

    this.queue.push(now);
    stompClient.send(destination, {}, JSON.stringify(message));
    return true;
  }
}
```

### 8-3. 메시지 크기 제한
- **최대 1MB**
- 초과 시 `MESSAGE_TOO_LARGE` 에러

### 8-4. 동시 접속 제한
- **프로젝트당 최대 24명**
- 초과 시 `CONNECTION_ERROR` (code: 4429)

### 8-5. Heartbeat
- **30초마다 Ping/Pong**
- 60초 미응답 시 연결 종료
- 재연결 시 Exponential Backoff 권장 (1s → 2s → 4s → 8s → 최대 30s)

```javascript
stompClient.connect({
  heartbeat: {
    outgoing: 30000,  // 30초
    incoming: 30000
  }
}, onConnect, onError);

function onError(error) {
  console.error('Connection error:', error);
  setTimeout(() => reconnect(), getBackoffDelay());
}

function getBackoffDelay() {
  return Math.min(1000 * Math.pow(2, retryCount), 30000);
}
```

---

## 9. 성능 최적화 팁

### 9-1. Throttling (커서/드래그)
```javascript
let lastSent = 0;
const THROTTLE_MS = 50;  // 20fps

function sendThrottled(data) {
  const now = Date.now();
  if (now - lastSent < THROTTLE_MS) return;

  stompClient.send(destination, {}, JSON.stringify(data));
  lastSent = now;
}
```

### 9-2. Batch Compression
- 서버는 2초 간격으로 Kafka 이벤트 배치 처리
- 동일 테이블/컬럼의 연속 변경은 하나로 압축됨

### 9-3. 이벤트 중복 제거
```javascript
const processedEvents = new Set();

stompClient.subscribe('/topic/project/123', (message) => {
  const event = JSON.parse(message.body);
  const eventId = `${event.type}_${event.event?.tableKey}_${event.event?.columnKey}`;

  if (processedEvents.has(eventId)) {
    console.log('Duplicate event ignored');
    return;
  }

  processedEvents.add(eventId);
  handleEvent(event);

  // 10초 후 제거
  setTimeout(() => processedEvents.delete(eventId), 10000);
});
```

---

## 10. 전체 플로우 예시

### 시나리오: 2명의 사용자가 동시에 ERD 편집

**사용자 A (EDITOR):**
1. 프로젝트 접속 → `CONNECTION_ESTABLISHED` 수신
2. 테이블 생성 (REST API) → `TableNewEvent` 수신
3. 테이블 드래그 시작 → `TableMoveEvent` 발행/수신 (실시간)
4. 테이블 드래그 완료 → `TableMoveEnd` 발행 (DB 저장만)
5. 테이블 더블클릭 → `TableLockEvent` 발행/수신
6. 컬럼 추가 (REST API) → `ColumnNewEvent` 수신
7. 편집 완료 → `TableUnlockEvent` 발행/수신

**사용자 B (EDITOR):**
1. 프로젝트 접속 → `CONNECTION_ESTABLISHED` + `MEMBER_JOIN` 수신
2. A가 만든 테이블 실시간 표시 → `TableNewEvent` 수신
3. A가 드래그하는 테이블 실시간 이동 → `TableMoveEvent` 수신
4. A가 편집 중인 테이블 잠금 표시 → `TableLockEvent` 수신
5. A가 추가한 컬럼 실시간 표시 → `ColumnNewEvent` 수신
6. A가 편집 완료 → `TableUnlockEvent` 수신
7. 커서 이동 → `CursorPosEvent` 발행/수신 (A의 커서도 표시됨)

**사용자 C (VIEWER):**
1. 프로젝트 접속 → `CONNECTION_ESTABLISHED` + `MEMBER_JOIN` 수신
2. 모든 이벤트 **구독만 가능** (발행 불가)
3. 편집 시도 시 → `ERROR` (FORBIDDEN) 수신

---

## 11. 디버깅 체크리스트

### 연결 안될 때
- [ ] 쿠키에 `accessToken`이 있는가?
- [ ] JWT 토큰이 유효한가? (만료 확인)
- [ ] 프로젝트 접근 권한이 있는가?
- [ ] CORS 설정이 올바른가?

### 이벤트가 안올 때
- [ ] 올바른 토픽을 구독했는가? (`/topic/project/{projectKey}`)
- [ ] Heartbeat 응답이 오는가?
- [ ] Rate Limit에 걸렸는가?
- [ ] 메시지 크기가 1MB를 초과하지 않았는가?

### 잠금이 안될 때
- [ ] EDITOR 이상 권한인가?
- [ ] 다른 사용자가 이미 잠금을 획득했는가?
- [ ] Redis 연결이 정상인가?

---

## 12. 참고 문서

- WebSocket 메시지 레퍼런스: `07_WebSocket_메시지_레퍼런스.csv`
- REST API 레퍼런스: `06_API 레퍼런스.csv`
- ERD 실시간 협업 가이드: `../../Back-End/yaldi/docs/ERD_REALTIME_COLLABORATION.md`
