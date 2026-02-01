# Yaldi ERD API 연동규격서 v1.3.0

## 📚 문서 개요

Yaldi ERD API v1.3.0 연동규격서에 오신 것을 환영합니다. 이 문서는 ERD 모델링 및 실시간 협업 기능을 제공하는 Yaldi API에 대한 완전한 레퍼런스입니다.

**버전**: v1.3.0
**작성일**: 2025-11-16
**Base URL**: `https://api.yaldi.com/api/v1/erd`

---

## 🎯 주요 기능

### v1.3.0 신규 기능
- ✅ **SQL Export**: PostgreSQL, MySQL DDL Export 지원
- ✅ **테이블/컬럼/관계 CRUD**: 전체 REST API 지원
- ✅ **실시간 협업**: WebSocket 기반 실시간 이벤트
- ✅ **테이블 잠금**: Redis 기반 편집 잠금 (TTL 30초)
- ✅ **커서 공유**: 실시간 커서 위치 공유
- ✅ **컬럼 순서 변경**: 드래그 앤 드롭 지원

### 기존 기능
- ERD 프로젝트 관리
- 테이블/컬럼/관계 모델링
- 프로젝트 멤버 관리
- 편집 히스토리 추적

---

## 📁 문서 구조

### CSV 파일 목록

| 파일명 | 설명 | 행 수 |
|--------|------|-------|
| `06_ERD_API_레퍼런스.csv` | ERD REST API 전체 엔드포인트 | 11개 API |
| `07_WebSocket_API.csv` | WebSocket 실시간 이벤트 | 18개 이벤트 |

### 참고 문서

| 문서 | 경로 | 설명 |
|------|------|------|
| ERD SQL Export 가이드 | `../ERD_SQL_Export_Guide.md` | SQL Export 상세 가이드 |
| ERD 실시간 협업 가이드 | `../../Back-End/yaldi/docs/ERD_REALTIME_COLLABORATION.md` | WebSocket 협업 상세 가이드 |
| 인증 패턴 가이드 | `../../Back-End/yaldi/docs/AUTHENTICATION_PATTERN.md` | 인증/인가 상세 가이드 |

---

## 🚀 빠른 시작

### 1. 인증

모든 API는 JWT 토큰 기반 인증을 사용합니다. JWT는 HttpOnly 쿠키로 전달됩니다.

```
Cookie: accessToken={accessToken}
```

### 2. REST API 호출 예시

#### 테이블 생성
```bash
curl -X POST https://api.yaldi.com/api/v1/erd/projects/100/tables \
  -H 'Content-Type: application/json' \
  --cookie 'accessToken={token}' \
  -d '{
    "logicalName": "회원",
    "physicalName": "members",
    "xPosition": 100.0,
    "yPosition": 200.0,
    "colorHex": "FF6B6B"
  }'
```

#### 컬럼 생성
```bash
curl -X POST https://api.yaldi.com/api/v1/erd/tables/100/columns \
  -H 'Content-Type: application/json' \
  --cookie 'accessToken={token}' \
  -d '{
    "logicalName": "회원 아이디",
    "physicalName": "member_id",
    "dataType": "BIGINT",
    "isPrimaryKey": true,
    "isIncremental": true
  }'
```

#### SQL Export
```bash
curl -X GET 'https://api.yaldi.com/api/v1/erd/projects/100/export/sql?dialect=POSTGRESQL' \
  --cookie 'accessToken={token}'
```

### 3. WebSocket 연결 예시

```javascript
// 1. SockJS 연결 (쿠키 인증 자동 사용)
const socket = new SockJS('https://api.yaldi.com/ws');
const stompClient = Stomp.over(socket);

// 2. 연결 및 구독
stompClient.connect({}, function(frame) {
  console.log('Connected: ' + frame);

  // 3. 프로젝트 토픽 구독
  stompClient.subscribe('/topic/project/100', function(message) {
    const event = JSON.parse(message.body);
    handleEvent(event);
  });
});

// 4. 이벤트 발행
function moveTable(tableKey, x, y) {
  stompClient.send('/pub/erd/table/move', {}, JSON.stringify({
    tableKey: tableKey,
    xPosition: x,
    yPosition: y
  }));
}

function lockTable(tableKey) {
  stompClient.send('/pub/erd/table/lock', {}, JSON.stringify({
    tableKey: tableKey
  }));
}
```

---

## 📊 API 개요

### REST API 엔드포인트 요약

| 카테고리 | 엔드포인트 수 | 주요 기능 |
|---------|-------------|----------|
| ERD 조회 | 1 | 프로젝트 ERD 전체 조회 |
| 테이블 관리 | 3 | 생성, 수정, 삭제 |
| 컬럼 관리 | 3 | 생성, 수정, 삭제 |
| 관계 관리 | 3 | 생성, 수정, 삭제 |
| SQL Export | 1 | PostgreSQL, MySQL DDL |

**총 11개 REST API 엔드포인트**

### WebSocket 이벤트 요약

| 이벤트 분류 | 이벤트 수 | 특징 |
|-----------|----------|------|
| A (REST+Kafka) | 11 | REST API 호출 시 자동 브로드캐스트 |
| B (WebSocket+Kafka) | 3 | WebSocket 발행 후 Kafka 전파 |
| C (WebSocket Only) | 2 | 휘발성, 즉시 브로드캐스트 |
| D (WebSocket Only) | 1 | 완전 휘발성, 커서 공유 |

**총 17개 WebSocket 이벤트**

---

## 🔐 보안

### 인증 방식
- **JWT 토큰**: HttpOnly 쿠키 방식 (XSS 방어)
- **WebSocket 인증**: 쿠키 기반 자동 인증

### 권한 체계
- **프로젝트 접근 권한**: 프로젝트 멤버만 접근 가능
- **편집 권한**: Editor 이상 권한 필요
- **삭제 권한**: Owner 권한 필요

---

## 📖 상세 API 레퍼런스

### ERD REST API

상세한 API 스펙은 `06_ERD_API_레퍼런스.csv` 파일을 참조하세요.

#### 주요 엔드포인트

1. **프로젝트 ERD 조회**
   - `GET /api/v1/erd/projects/{projectKey}`
   - 프로젝트의 전체 ERD 데이터 조회

2. **테이블 생성**
   - `POST /api/v1/erd/projects/{projectKey}/tables`
   - WebSocket `TableNewEvent` 자동 브로드캐스트

3. **테이블 수정**
   - `PATCH /api/v1/erd/tables/{tableKey}`
   - WebSocket `TableLnameEvent`, `TablePnameEvent`, `TableColorEvent` 브로드캐스트

4. **컬럼 생성**
   - `POST /api/v1/erd/tables/{tableKey}/columns`
   - WebSocket `ColumnNewEvent` 자동 브로드캐스트

5. **관계 생성**
   - `POST /api/v1/erd/projects/{projectKey}/relations`
   - WebSocket `RelationNewEvent` 자동 브로드캐스트

6. **SQL Export**
   - `GET /api/v1/erd/projects/{projectKey}/export/sql?dialect={POSTGRESQL|MYSQL}`
   - PostgreSQL, MySQL DDL 생성

### WebSocket API

상세한 이벤트 스펙은 `07_WebSocket_API.csv` 파일을 참조하세요.

#### 이벤트 타입 분류

**A 타입: REST API + Kafka 브로드캐스트**
- REST API 호출 시 자동으로 WebSocket 브로드캐스트
- 예: `TableNewEvent`, `ColumnNewEvent`, `RelationNewEvent`

**B 타입: WebSocket + Kafka 브로드캐스트**
- WebSocket 메시지 전송 후 Kafka로 전파
- 예: `ColumnOrderEvent`, `TableLockEvent`, `TableUnlockEvent`

**C 타입: WebSocket Only (휘발성)**
- 즉시 브로드캐스트, DB/Redis 저장 없음
- 예: `TableMoveEvent`

**D 타입: WebSocket Only (완전 휘발성)**
- 완전 휘발성, Kafka 없이 즉시 브로드캐스트
- 예: `CursorPosEvent`

#### 연결 및 구독

```javascript
// 연결
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// 구독
stompClient.subscribe('/topic/project/100', function(message) {
  const broadcastEvent = JSON.parse(message.body);
  console.log('Event:', broadcastEvent.event.type);
});
```

#### 이벤트 발행

```javascript
// 테이블 이동 (드래그 중)
stompClient.send('/pub/erd/table/move', {}, JSON.stringify({
  tableKey: 10,
  xPosition: 150.5,
  yPosition: 220.3
}));

// 테이블 이동 완료
stompClient.send('/pub/erd/table/move/end', {}, JSON.stringify({
  tableKey: 10,
  xPosition: 150.5,
  yPosition: 220.3
}));

// 컬럼 순서 변경
stompClient.send('/pub/erd/column/reorder', {}, JSON.stringify({
  columnKey: 101,
  columnOrder: 2
}));

// 테이블 잠금
stompClient.send('/pub/erd/table/lock', {}, JSON.stringify({
  tableKey: 10
}));

// 테이블 잠금 해제
stompClient.send('/pub/erd/table/unlock', {}, JSON.stringify({
  tableKey: 10
}));

// 커서 위치 공유
stompClient.send('/pub/erd/cursor', {}, JSON.stringify({
  projectKey: 100,
  xPosition: 350.5,
  yPosition: 420.8
}));
```

---

## 🗂️ 데이터 타입

### RelationType (관계 타입)
- `ONE_TO_ONE`: 1:1 관계
- `ONE_TO_MANY`: 1:N 관계
- `MANY_TO_ONE`: N:1 관계
- `MANY_TO_MANY`: N:M 관계

### ReferentialActionType (참조 동작)
- `CASCADE`: 연쇄 삭제/수정
- `SET_NULL`: NULL로 설정
- `SET_DEFAULT`: 기본값으로 설정
- `RESTRICT`: 제한 (자식이 있으면 불가)
- `NO_ACTION`: 아무 동작 안 함

### SqlDialect (SQL 방언)
- `POSTGRESQL`: PostgreSQL DDL (기본값)
- `MYSQL`: MySQL DDL

### 데이터 타입 매핑 (SQL Export)

| 개념 | PostgreSQL | MySQL |
|------|-----------|-------|
| 정수 | BIGINT | BIGINT |
| 가변문자 | VARCHAR(n) | VARCHAR(n) |
| 고정소수 | NUMERIC(p,s) | DECIMAL(p,s) |
| 타임스탬프 | TIMESTAMP | DATETIME |
| JSON | JSONB | JSON |
| 배열 | TEXT[] | JSON (자동 변환) |
| UUID | UUID | CHAR(36) |

---

## ❌ 에러 코드

### 공통 에러

| 코드 | HTTP 상태 | 메시지 |
|-----|----------|-------|
| COMMON200 | 200 | 성공입니다. |
| COMMON400 | 400 | 잘못된 요청입니다. |
| COMMON401 | 401 | 인증되지 않은 사용자입니다. |
| COMMON403 | 403 | 권한이 없습니다. |
| COMMON404 | 404 | 리소스를 찾을 수 없습니다. |
| COMMON500 | 500 | 서버 내부 오류입니다. |

### ERD 도메인 에러

| 코드 | HTTP 상태 | 메시지 |
|-----|----------|-------|
| ERD001 | 404 | 테이블을 찾을 수 없습니다. |
| ERD002 | 404 | 컬럼을 찾을 수 없습니다. |
| ERD003 | 404 | 관계를 찾을 수 없습니다. |
| ERD004 | 400 | 중복된 물리명입니다. |
| ERD005 | 400 | 잘못된 관계 설정입니다. |
| ERD006 | 409 | 테이블이 잠겨 있습니다. |
| ERD007 | 400 | 지원하지 않는 SQL 방언입니다. |

---

## 📝 SQL Export 가이드

### 지원 기능

1. **CREATE TABLE 문 생성**
   - 테이블 정의 (물리적 테이블명)
   - 컬럼 정의 (데이터 타입, 제약조건)
   - PRIMARY KEY 제약조건
   - 테이블/컬럼 코멘트

2. **컬럼 제약조건**
   - NOT NULL
   - UNIQUE
   - PRIMARY KEY
   - DEFAULT
   - AUTO_INCREMENT (MySQL) / SERIAL (PostgreSQL)

3. **외래키 제약조건**
   - FOREIGN KEY 정의
   - ON DELETE, ON UPDATE 액션
   - 제약조건 이름 자동 생성

### 제약사항

**미지원 기능**
- ENUM 타입
- CHECK 제약조건
- INDEX 생성
- TRIGGER
- VIEW
- SEQUENCE

**데이터베이스별 제한**
- PostgreSQL: Identifier 최대 63자
- MySQL: Identifier 최대 64자, 배열 타입 → JSON 자동 변환

상세한 Export 가이드는 [ERD SQL Export 가이드](../ERD_SQL_Export_Guide.md)를 참조하세요.

---

## 🔄 실시간 협업

### 테이블 잠금 메커니즘

```javascript
// 테이블 편집 시작 시 잠금
stompClient.send('/pub/erd/table/lock', {}, JSON.stringify({
  tableKey: 10
}));

// 편집 완료 시 잠금 해제
stompClient.send('/pub/erd/table/unlock', {}, JSON.stringify({
  tableKey: 10
}));
```

**잠금 특징**
- Redis TTL 30초 (자동 만료)
- WebSocket 연결 끊김 시 자동 해제
- 잠금 실패 시 다른 사용자 편집 중 표시

### 커서 위치 공유

```javascript
// 마우스 이동 시 주기적 전송 (throttle 50-100ms 권장)
let lastCursorSent = 0;
canvas.addEventListener('mousemove', (e) => {
  const now = Date.now();
  if (now - lastCursorSent > 50) {
    stompClient.send('/pub/erd/cursor', {}, JSON.stringify({
      projectKey: 100,
      xPosition: e.clientX,
      yPosition: e.clientY
    }));
    lastCursorSent = now;
  }
});
```

**커서 특징**
- 완전 휘발성 (DB/Redis 저장 없음)
- 사용자별 자동 색상 생성 (이메일 해시 기반)
- 실시간 브로드캐스트만 수행

---

## 📚 참고 자료

### 관련 문서
- [ERD SQL Export 가이드](../ERD_SQL_Export_Guide.md)
- [ERD 실시간 협업 가이드](../../Back-End/yaldi/docs/ERD_REALTIME_COLLABORATION.md)
- [인증 패턴 가이드](../../Back-End/yaldi/docs/AUTHENTICATION_PATTERN.md)
- [성능 최적화 가이드](../../Back-End/yaldi/docs/PERFORMANCE_OPTIMIZATION.md)

### 외부 참고
- [PostgreSQL Data Types](https://www.postgresql.org/docs/current/datatype.html)
- [MySQL Data Types](https://dev.mysql.com/doc/refman/8.0/en/data-types.html)
- [STOMP Protocol](https://stomp.github.io/)
- [SockJS Documentation](https://github.com/sockjs/sockjs-client)

---

## 🔖 버전 히스토리

### v1.3.0 (2025-11-16)
- ✅ SQL Export 기능 추가 (PostgreSQL, MySQL)
- ✅ 테이블/컬럼/관계 REST API 정식 지원
- ✅ WebSocket 실시간 협업 기능 강화
- ✅ 테이블 잠금 기능 추가 (Redis TTL 30초)
- ✅ 컬럼 순서 변경 기능 추가
- ✅ 커서 위치 공유 기능 추가
- ✅ 이벤트 타입 분류 체계 정립 (A/B/C/D 타입)

### v1.2.0 (2025-10-01)
- WebSocket 기반 실시간 협업 도입
- Kafka 이벤트 브로드캐스팅

### v1.1.0 (2025-08-01)
- 기본 ERD CRUD API 제공
- 프로젝트별 ERD 관리
