# Yaldi API 연동 규격서

## 📋 문서 개요

Yaldi ERD 도구의 백엔드 API 연동 규격서입니다. 프론트엔드와 백엔드 개발자 간 명확한 커뮤니케이션을 위한 표준 문서로, 섹션별로 구분된 CSV 파일로 제공됩니다.

**버전**: v1.1.0
**작성자**: 최경민
**작성일**: 2024-10-20

---

## 📚 파일 구조

### 1. 기본 정보

| 파일명 | 행수 | 설명 |
|--------|------|------|
| 01_개요.csv | 6행 | 프로젝트 개요, 목적, 기술 스택 정보 |
| 02_시작하기.csv | 5행 | 사전작업, 인증방식, 실시간 협업 구조 |
| 03_보안 및 통신.csv | 17행 | HTTPS/TLS, WebSocket, SSE 보안 설정 |
| 04_보안 및 인증.csv | 14행 | 세션 쿠키, OAuth 2.0, RBAC, Audit Log, CSRF 보호 |
| 05_보안 코딩.csv | 7행 | CORS, Security Headers, Input Validation, 암호화 |

### 2. API 명세

| 파일명 | 행수 | 설명 |
|--------|------|------|
| **06_API 레퍼런스.csv** | **108행** | **전체 API 엔드포인트 상세 명세 (메인 문서)** |
| 07_RESTful 아키텍처.csv | 3행 | HATEOAS, ETag 캐싱, 낙관적 잠금 예제 |
| 08_API 설계.csv | 35행 | 에러 코드, Pagination, Webhook, Rate Limiting, SLA |
| 09_공통 에러 코드.csv | 14행 | HTTP 상태 코드 14개 (200~503) |

### 3. 참고 자료

| 파일명 | 행수 | 설명 |
|--------|------|------|
| 10_예제.csv | 7행 | API 사용 예제 코드 (JavaScript, Python 등) |
| 11_용어 설명.csv | 43행 | 기술 용어 사전 (JWT, OAuth, RBAC, HATEOAS 등) |
| 12_버전 관리.csv | 2행 | API 버전 정보 및 변경 이력 |
| 13_OpenAPI 호환.csv | 4행 | OpenAPI 3.0 스펙, API 서버 URL |
| 14_클라우드 운영.csv | 4행 | Health Check, Metrics, Logging, Microservice 패턴 |
| 15_참고 문서.csv | 2행 | 공식 문서 링크 및 참고 자료 |

---

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.x (Java 17+)
- **Port**: 8080
- **Database**: PostgreSQL / MySQL
- **Cache**: Redis
- **Storage**: AWS S3

### Frontend (가정)
- **Framework**: React 18.x
- **Port**: 3000
- **OAuth Callback**: http://localhost:3000 (프론트엔드에서 처리)

### Realtime
- **Owner/Editor**: WebSocket (양방향 실시간 편집)
- **Viewer**: SSE (단방향 실시간 수신)

---

## 🌐 환경별 API 서버 URL

| 환경 | URL | 포트 |
|------|-----|------|
| **Production** | https://api.yaldi.io | 443 |
| **Staging** | https://api-staging.yaldi.io | 443 |
| **Development** | https://api-dev.yaldi.io | 443 |
| **Local** | http://localhost:8080 | 8080 |

---

## 📖 주요 섹션 설명

### 01_개요.csv
프로젝트의 목적, 배경, 비즈니스 필요성을 설명합니다. 기술 스택 정보도 포함되어 있습니다.

**주요 항목**: 목적, 배경, 비즈니스 필요성, 작성자, 기술 스택, OpenAPI Spec

### 06_API 레퍼런스.csv (⭐ 메인 문서)
108개의 API 엔드포인트에 대한 상세 명세입니다.

**포함 정보**:
- HTTP 메소드, 엔드포인트, 인증 필요 여부
- 요청 헤더, 파라미터, 본문
- 응답 상태 코드, 응답 본문
- 예제 요청/응답
- 비고 및 버전

**카테고리**:
- 사용자 인증 (회원가입, 로그인, OAuth 소셜 로그인)
- 프로젝트 관리 (CRUD, 멤버 관리, 권한)
- ERD 관리 (테이블, 컬럼, 관계, DTO)
- 버전 관리 (버전 생성, 복원, 비교)
- 프로젝트 탐색 (검색, 필터링, 정렬)
- 내보내기 (SQL, JSON, PNG)
- 태블릿 스케치 (업로드, AI 변환)
- 실시간 협업 (WebSocket, SSE)

### 08_API 설계.csv
엔터프라이즈급 API 설계 표준 35가지를 정의합니다.

**주요 항목**:
- 에러 코드 체계 (AUTH_xxx, PROJ_xxx, ERD_xxx 등)
- Pagination 표준 (Offset-based, Cursor-based)
- 필터링/정렬/검색 쿼리 표준
- Idempotency Key
- Webhook 이벤트 시스템
- Rate Limiting 정책
- Batch API 설계
- API 버전 관리 및 Deprecation 정책
- SLA/SLO 명시
- JSON Schema 정의
- Request/Response 압축
- CORS 정책 상세

### 11_용어 설명.csv
43개의 기술 용어에 대한 설명을 제공합니다.

**카테고리**:
- 인증/인가: 세션 쿠키, HttpOnly, Secure, SameSite, OAuth 2.0, PKCE, CSRF 토큰
- 권한 관리: RBAC, Permission, Audit Log
- RESTful: HATEOAS, ETag, Idempotency, Optimistic Locking
- 보안: CORS, CSP, HSTS, XSS, CSRF, SQL Injection, bcrypt, TLS 1.3
- 운영: Liveness Probe, Prometheus, Grafana, ELK Stack, Circuit Breaker
- OpenAPI: OpenAPI 3.0, Swagger UI, servers, components/schemas

---

## 🔐 보안 정책

### 인증
- **세션 쿠키**: yaldi_session_id, HttpOnly, Secure, SameSite=Lax
- **세션 유효 기간**: 1시간 (활동 시 자동 갱신), Remember Me 선택 시 30일
- **세션 저장소**: Redis (인메모리 캐시)
- **OAuth 2.0**: Google, GitHub 지원
- **PKCE**: 모바일/SPA 환경 보안 강화
- **CSRF 보호**: X-CSRF-Token 헤더 (변경 작업 시 필수)

### 권한 (RBAC)
- **Owner**: 모든 권한 (삭제, 멤버 관리)
- **Editor**: 읽기/쓰기 (ERD 편집, 버전 생성)
- **Viewer**: 읽기 전용

### 보안 헤더
- Strict-Transport-Security (HSTS)
- X-Content-Type-Options
- X-Frame-Options
- Content-Security-Policy (CSP)

### 암호화
- **In-Transit**: TLS 1.3 (최소 1.2)
- **At-Rest**: AES-256
- **비밀번호**: bcrypt (cost factor 12)

---

## 📊 API 설계 표준

### Rate Limiting
- 인증된 사용자: 1000 req/hour
- 미인증 사용자: 100 req/hour
- 로그인 시도: 5 req/15min

### Pagination
- **Offset-based**: `?page=1&limit=20&sort=-createdAt`
- **Cursor-based**: `?cursor=xxx&limit=20`

### 에러 코드 체계
```json
{
  "success": false,
  "error": {
    "code": "ERD_004",
    "type": "CircularReferenceError",
    "message": "순환 참조가 발생했습니다",
    "field": "relations",
    "details": { ... }
  }
}
```

---

## 🚀 사용 방법

### 1. CSV 파일 열기
Excel, Google Sheets, 또는 텍스트 에디터로 CSV 파일을 열 수 있습니다.

### 2. API 검색
`06_API 레퍼런스.csv`에서 필요한 API를 카테고리별로 찾습니다.

### 3. 용어 확인
모르는 기술 용어는 `11_용어 설명.csv`에서 찾아볼 수 있습니다.

### 4. 예제 코드 참고
`10_예제.csv`에서 JavaScript, Python 등의 예제 코드를 확인합니다.

---

## 📝 변경 이력

### v1.1.0 (2024-10-20)
- API 설계 표준 12가지 추가 (에러 코드, Pagination, Webhook 등)
- 용어 설명 43개 추가
- 보안 코딩, 클라우드 운영 섹션 추가
- 공통 에러 코드 14개 정의 (200~503)
- 파일 순서 번호 부여 (01~15)

### v1.0.0 (초기 릴리스)
- 기본 API 엔드포인트 108개 정의
- 인증, 권한, 보안 정책 수립

---

## 🔗 참고 문서

- **OpenAPI Specification**: https://spec.openapi.org/oas/v3.0.3
- **JWT**: https://jwt.io
- **OAuth 2.0**: https://oauth.net/2/
- **Yaldi API Documentation**: https://docs.yaldi.io
- **Yaldi Status Page**: https://status.yaldi.io

---

## 📧 문의

API 규격서에 대한 문의사항은 아래로 연락주세요.

- **이메일**: support@yaldi.io
- **GitHub**: https://github.com/yaldi-io

---

**© 2024 Yaldi. All rights reserved.**
