# Yaldi 프로젝트 기술 스택

## 목차
1. [프론트엔드](#1-프론트엔드)
2. [백엔드](#2-백엔드)
3. [데이터베이스](#3-데이터베이스)
4. [AI/ML](#4-aiml)
5. [메시징 및 스트리밍](#5-메시징-및-스트리밍)
6. [모니터링 및 관찰성](#6-모니터링-및-관찰성)
7. [인프라 및 DevOps](#7-인프라-및-devops)
8. [실시간 통신](#8-실시간-통신)
9. [주요 서비스 포트](#9-주요-서비스-포트)
10. [프로젝트 특징](#10-프로젝트-특징)

---

## 1. 프론트엔드

### 위치
`Front-End/Yaldi`

### 1.1. 프론트엔드 프레임워크

#### React 19.1.1
| 기술 | 버전 | 용도 |
|------|------|------|
| **react** | 19.1.1 | UI 프레임워크 (최신 버전) |
| **react-dom** | 19.1.1 | React DOM 렌더링 |

**특징**:
- JSX 런타임: react-jsx (자동 JSX 변환)
- 최신 React 기능 활용

#### TypeScript 5.9.3
- **컴파일러**: TypeScript v5.9.3
- **엄격 모드**: 활성화 (strict mode)
- **타입 정의**: 각 모듈별 타입 선언 (`src/types/`)
- **컴파일 타임 타입 체크**로 런타임 오류 사전 방지

---

### 1.2. 빌드 도구

#### Vite 7.1.7
**설정 파일**: `vite.config.ts`

**주요 플러그인**:
- `@vitejs/plugin-react` v5.0.4 - React 지원
- `vite-plugin-svgr` v4.5.0 - SVG를 React 컴포넌트로 변환

**최적화 설정**:
```typescript
optimizeDeps: {
  include: ['sockjs-client', '@stomp/stompjs']
}
```
- SockJS 및 STOMP.js 사전 번들링으로 로딩 성능 향상
- 빠른 HMR (Hot Module Replacement)
- 코드 스플리팅 및 트리 쉐이킹

---

### 1.3. 상태 관리

#### Zustand 5.0.8
경량 상태 관리 라이브러리

**주요 Store 목록**:
| Store | 파일 경로 | 역할 |
|-------|----------|------|
| authStore | `src/stores/authStore.ts` | 인증 및 사용자 정보 관리 |
| notificationStore | `src/stores/notificationStore.ts` | 알림 목록 및 SSE 연결 상태 |
| dataModelStore | `src/stores/dataModelStore.ts` | 데이터 모델 상태 |
| entitySelectionStore | `src/stores/entitySelectionStore.ts` | 엔티티 선택 상태 |
| dtoSelectionStore | `src/stores/dtoSelectionStore.ts` | DTO 선택 상태 |
| aiDraftStore | `src/stores/aiDraftStore.ts` | AI 드래프트 관련 상태 |

**Persist 미들웨어**:
- **저장소**: sessionStorage 사용
- **용도**: 페이지 새로고침 시 상태 유지

---

### 1.4. 라우팅

#### React Router DOM 7.9.4
**설정 파일**: `src/App.tsx`

**라우팅 구조**:
- **BrowserRouter**: HTML5 History API 기반
- **중첩 라우팅(Nested Routes)**: 레이아웃 패턴 활용

**레이아웃 패턴**:
1. **MainHeader 레이아웃**: 일반 페이지 (워크스페이스, 알림 등)
2. **ErdHeader 레이아웃**: ERD 편집기 전용
3. **헤더 없는 레이아웃**: 로그인, 뷰어 페이지

**코드 스플리팅**:
- **React.Suspense**: 지연 로딩으로 초기 번들 크기 최적화

---

### 1.5. HTTP 클라이언트

#### Axios 1.12.2
**설정 파일**: `src/apis/apiController.ts`

**주요 설정**:
```typescript
{
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,  // 쿠키 인증 사용
  timeout: 5000
}
```

**인터셉터**:
- **401 에러 처리**: 자동 로그아웃 및 로그인 페이지 리다이렉션
- **에러 핸들링**: 중앙 집중식 에러 처리

---

### 1.6. 스타일링

#### Tailwind CSS 3.4.18
**설정 파일**: `tailwind.config.js`

**기술 스택**:
- **Tailwind CSS** v3.4.18 - 유틸리티 퍼스트 CSS 프레임워크
- **PostCSS** v8.5.6 - CSS 후처리
- **Autoprefixer** v10.4.21 - CSS 벤더 프리픽스 자동 추가

**커스텀 테마**:
```javascript
colors: {
  blue: { 1: '#1A56DB', ... },
  'light-blue': { ... },
  'my-black': { 1: '#111827', ... },
  'my-white': { 1: '#F9FAFB', ... }
}
```

**폰트**:
- **Pretendard**: 기본 폰트 패밀리

**커스텀 애니메이션**:
- `fadeIn`: 페이드인 효과

**커스텀 컬러 스킴**:
- 사용자 아바타 색상 팔레트
- 엔티티 타입별 구분 색상
- 브랜드 컬러 시스템

---

### 1.7. UI 라이브러리

#### SweetAlert2 11.26.3
- **용도**: 모달 및 알림 다이얼로그
- **커스터마이징**: 프로젝트 디자인 시스템에 맞춘 스타일

---

### 1.8. 실시간 통신 (프론트엔드)

#### WebSocket (STOMP)
| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| **@stomp/stompjs** | 7.2.1 | STOMP over WebSocket |
| **sockjs-client** | 1.6.1 | WebSocket 폴백 |

**구현 파일**: `src/services/websocket/ErdWebSocketClient.ts`
**용도**: ERD 편집 실시간 협업

#### Server-Sent Events (SSE)
**커스텀 훅**:
- `useSSENotification.ts`: 알림 스트리밍
- `useSSEViewer.ts`: 뷰어 실시간 업데이트

**용도**: 서버→클라이언트 단방향 알림

---

### 1.9. 기타 주요 라이브러리

#### 유틸리티
- **html-to-image** v1.11.11
  - HTML 요소를 이미지로 변환
  - ERD 캡처 및 내보내기 기능

#### 개발 도구
**ESLint 9.36.0**:
- React Hooks 규칙
- React Refresh
- TypeScript ESLint v8.45.0

**Prettier**:
- **설정 파일**: `.prettierrc.json`
- **통일된 코드 스타일** 적용

---

### 1.10. 프로젝트 구조

```
src/
├── App.tsx                 # 라우팅 설정
├── main.tsx               # 애플리케이션 진입점
│
├── apis/                  # API 레이어
│   ├── apiController.ts   # Axios 인스턴스 및 인터셉터
│   ├── searchApi.ts       # 검색 API
│   └── tableApi.ts        # 테이블 API
│
├── assets/               # 정적 리소스 (이미지, 아이콘 등)
│
├── components/           # 재사용 가능한 컴포넌트
│   └── common/          # 공통 컴포넌트 (헤더, 버튼 등)
│
├── contexts/            # React Context (전역 상태)
│
├── hooks/               # 커스텀 훅 (15개)
│   ├── useSSENotification.ts    # SSE 알림
│   ├── useSSEViewer.ts          # SSE 뷰어
│   ├── useCreateProject.ts      # 프로젝트 생성
│   └── ...
│
├── pages/               # 페이지 컴포넌트
│   ├── login/          # 로그인
│   ├── workspace/      # 워크스페이스 (ERD 편집)
│   ├── dataModel/      # 데이터 모델
│   ├── version/        # 버전 관리
│   ├── notification/   # 알림
│   └── search/         # 검색
│
├── services/            # 비즈니스 로직
│   └── websocket/       # WebSocket 서비스
│       └── ErdWebSocketClient.ts
│
├── stores/              # Zustand 스토어 (7개)
│   ├── authStore.ts
│   ├── notificationStore.ts
│   └── ...
│
├── styles/              # 전역 스타일
│   └── global.css
│
├── types/               # TypeScript 타입 정의
│
└── utils/               # 유틸리티 함수
```

---

### 1.11. 프론트엔드 주요 기능

#### 1. 실시간 협업
- **기술**: STOMP over WebSocket
- **구현**: `ErdWebSocketClient.ts`
- **기능**: ERD 편집 내용 실시간 동기화

#### 2. 실시간 알림
- **기술**: Server-Sent Events (SSE)
- **구현**: `useSSENotification.ts`
- **기능**: 서버 이벤트를 클라이언트에 스트리밍

#### 3. 인증/인가
- **방식**: 세션 기반 인증
- **쿠키**: `withCredentials: true`
- **자동 로그아웃**: 401 에러 발생 시 처리

#### 4. 상태 지속성
- **기술**: Zustand persist 미들웨어
- **저장소**: sessionStorage
- **용도**: 새로고침 시 상태 유지

#### 5. 타입 안정성
- **TypeScript strict 모드**
- 컴파일 타임 타입 체크로 런타임 오류 감소

#### 6. 모던 빌드
- **Vite**: 빠른 HMR (Hot Module Replacement)
- **번들 최적화**: 코드 스플리팅 및 트리 쉐이킹

#### 7. 일관된 UI/UX
- **Tailwind CSS**: 유틸리티 우선 CSS
- **디자인 시스템**: 커스텀 컬러 팔레트 및 애니메이션

---

### 1.12. 프론트엔드 아키텍처 특징

- **관심사 분리**: API, 비즈니스 로직, UI 레이어 명확한 분리
- **컴포넌트 재사용**: 공통 컴포넌트 및 커스텀 훅 활용
- **타입 안정성**: TypeScript strict 모드로 타입 오류 사전 방지

---

### 1.13. 환경 변수

**`.env` 파일**:
```bash
VITE_API_BASE_URL=<API 서버 주소>
```

**사용 예시**:
```typescript
const baseURL = import.meta.env.VITE_API_BASE_URL;
```

---

## 2. 백엔드

### 위치
`Back-End/yaldi`

### 프레임워크
| 기술 | 버전 | 용도 |
|------|------|------|
| **Spring Boot** | 3.5.7 | 백엔드 프레임워크 |
| **Java** | 21 (LTS) | 프로그래밍 언어 |
| **Gradle** | - | 빌드 도구 |

### 주요 Spring Boot Starters
- `spring-boot-starter-web` - REST API 개발
- `spring-boot-starter-data-jpa` - JPA/Hibernate ORM
- `spring-boot-starter-data-redis` - Redis 캐싱
- `spring-boot-starter-data-elasticsearch` - Elasticsearch 통합
- `spring-boot-starter-websocket` - WebSocket 실시간 통신
- `spring-boot-starter-security` - 보안 및 인증
- `spring-boot-starter-oauth2-client` - OAuth2 소셜 로그인
- `spring-boot-starter-validation` - 데이터 검증
- `spring-boot-starter-mail` - 이메일 발송
- `spring-boot-starter-webflux` - 비동기 HTTP 클라이언트
- `spring-boot-starter-actuator` - 애플리케이션 모니터링

### 보안 및 인증
| 기술 | 버전 | 용도 |
|------|------|------|
| **JWT** | 0.12.6 | JSON Web Token 인증 |
| **OAuth2** | - | Google, GitHub 소셜 로그인 |
| **Spring Security** | - | 통합 보안 솔루션 |

### 메시징 및 이벤트
- **Spring Kafka** - 이벤트 스트리밍 및 메시지 큐
- **Redisson** 3.27.2 - Redis 분산 락 및 객체

### API 문서
- **Springdoc OpenAPI** 2.8.4 - Swagger UI 자동 생성

### 데이터베이스 관련
- **Flyway** - 데이터베이스 마이그레이션 도구
- **Hypersistence Utils** 3.7.3 - PostgreSQL JSONB 최적화
- **PostgreSQL Driver** 42.7.7

### 클라우드 통합
- **AWS SDK S3** 2.20.0 - S3 파일 저장소

### 모니터링
- **Micrometer + Prometheus** - 메트릭 수집 및 노출
- **Spring Actuator** - 헬스체크 및 애플리케이션 상태

### 기타 주요 라이브러리
- **Lombok** - 보일러플레이트 코드 감소
- **Jackson** - JSON 직렬화/역직렬화
- **ULID Creator** 5.2.3 - 분산 환경용 고유 ID 생성
- **Spring DotEnv** 4.0.0 - 환경 변수 관리

### 아키텍처
- **설계 패턴**: DDD (Domain-Driven Design)
- **계층 구조**: Domain Layer, Infrastructure Layer, Global Layer
- **주요 도메인**: User, Team, Project, ERD, Version, DataModel, Comment, Notification, Agent

---

## 3. 데이터베이스

### 주요 데이터베이스
| 데이터베이스 | 버전 | 포트 | 용도 |
|--------------|------|------|------|
| **PostgreSQL** | 16 (pgvector) | 5432 | 메인 RDBMS, 벡터 검색 |
| **Redis** | 7-alpine | 6379 | 캐싱, 세션, Pub/Sub |
| **Elasticsearch** | 8.18.0 | 9200, 9300 | 전문 검색 엔진 |
| **Neo4j** | 5.15-community | 7474, 7687 | 그래프 데이터베이스 (Graph RAG) |

### PostgreSQL 특징
- **확장**: pgvector - 벡터 임베딩 저장 및 검색
- **드라이버**: postgresql 42.7.7
- **용도**: 메인 데이터 저장, 벡터 유사도 검색

### Redis 특징
- **용도**:
  - 세션 관리
  - 캐싱 레이어
  - WebSocket Pub/Sub 메시지 브로커

### Elasticsearch 특징
- **버전**: 8.18.0
- **용도**: ERD, 프로젝트, 버전 등 전문 검색

### Neo4j 특징
- **버전**: 5.15-community
- **플러그인**: APOC, Graph Data Science
- **용도**: Graph RAG, 지식 그래프, AI 서비스 연동

---

## 4. AI/ML

### 위치
`AI/yaldi`

### 웹 프레임워크
| 기술 | 버전 | 용도 |
|------|------|------|
| **FastAPI** | 0.109.0 | 비동기 웹 프레임워크 |
| **Uvicorn** | 0.27.0 | ASGI 서버 |
| **Pydantic** | 2.5.3 | 데이터 검증 및 직렬화 |

### LLM 및 AI 프레임워크
| 기술 | 버전 | 용도 |
|------|------|------|
| **OpenAI** | 1.10.0 | GPT-4o API 클라이언트 |
| **LangChain** | 0.1.0+ | LLM 애플리케이션 프레임워크 |
| **LangChain-OpenAI** | 0.0.5+ | OpenAI 통합 |
| **LangChain-Community** | 0.0.20+ | 커뮤니티 통합 |
| **LangGraph** | 0.0.26+ | 그래프 기반 Multi-Agent 워크플로우 |
| **LangSmith** | 0.1.0+ | LLM 애플리케이션 모니터링 |

### LLM 설정
- **모델**: GPT-4o
- **API**: SSAFY GMS (https://gms.ssafy.io)
- **Temperature**:
  - 기본: 0.7
  - ERD 생성: 0.3 (일관성 향상)

### 데이터베이스 드라이버
- **asyncpg** 0.29.0 - PostgreSQL 비동기 드라이버
- **aiomysql** 0.2.0 - MySQL 비동기 드라이버
- **neo4j** 5.16.0 - Neo4j Python 드라이버

### HTTP 클라이언트
- **httpx** 0.26.0 - 비동기 HTTP 클라이언트

### 기타 라이브러리
- **cryptography** 41.0.7 - 암호화
- **sqlparse** 0.4.4 - SQL 파싱
- **python-dotenv** 1.0.1 - 환경 변수 관리

### AI 주요 기능

#### 1. Multi-Agent System (LangGraph)
**ERD 생성 에이전트**:
- Domain Analyst - 도메인 분석
- Schema Architect - 스키마 설계
- Validator - 검증
- Optimizer - 최적화
- Meta Agent - 전체 조율

**상담 챗봇 에이전트**:
- Intent Router - 의도 분류
- Expert Agents - 전문 상담
- Context Enrichment - 컨텍스트 보강

#### 2. 기타 AI 기능
- Mock 데이터 생성
- ERD Import 검증
- Graph RAG (Neo4j 기반 지식 그래프)
- Vector Search (버전 임베딩 유사도 검색)

### 런타임
- **Python**: 3.11-slim
- **Workers**: 2 (Uvicorn)

---

## 5. 메시징 및 스트리밍

### Apache Kafka
| 항목 | 내용 |
|------|------|
| **버전** | 3.8.1 (KRaft 모드) |
| **포트** | 9092 (내부), 9093 (외부) |
| **모드** | KRaft (Zookeeper 불필요) |
| **용도** | 이벤트 스트리밍, 비동기 메시지 큐 |

### Kafka UI
- **제공자**: Provectus
- **포트**: 8989
- **용도**: Kafka 클러스터 모니터링 및 관리

---

## 6. 모니터링 및 관찰성

### Prometheus
- **버전**: latest
- **포트**: 9090
- **용도**: 메트릭 수집 및 시계열 데이터 저장

### Grafana
- **버전**: latest
- **포트**: 3000
- **기본 계정**: admin/admin
- **용도**: 메트릭 시각화 대시보드

### Spring Actuator
- **엔드포인트**: health, info, prometheus, metrics
- **경로**: `/actuator`
- **용도**: 애플리케이션 헬스체크 및 메트릭 노출

---

## 7. 인프라 및 DevOps

### 컨테이너화
- **Docker** - 컨테이너 런타임
- **Docker Compose** 3.8 - 멀티 컨테이너 오케스트레이션

### Java Runtime
- **Eclipse Temurin** 21-jre-alpine
- **GC**: G1GC (Garbage First Garbage Collector)
- **Heap**: 최대 2GB

### Python Runtime
- **Python** 3.11-slim

### 네트워크
- **Docker Network**: `yaldi-network` (bridge)

### 볼륨 관리
```
- postgres_data
- redis_data
- kafka_data
- prometheus_data
- grafana_data
- elasticsearch_data
- neo4j_data
- neo4j_logs
```

---

## 8. 실시간 통신

### WebSocket (백엔드)
- **프레임워크**: Spring WebSocket + STOMP
- **메시지 브로커**: Redis Pub/Sub
- **용도**:
  - 실시간 협업 (ERD 동시 편집)
  - 실시간 알림
  - 채팅

### Server-Sent Events (SSE)
- **프론트엔드 구현**: 커스텀 훅 (`useSSENotification.ts`, `useSSEViewer.ts`)
- **용도**: 서버→클라이언트 단방향 실시간 알림

---

## 9. 주요 서비스 포트

| 서비스 | 포트 | 용도 |
|--------|------|------|
| **Spring Boot** | 8080 | 백엔드 REST API |
| **Swagger UI** | 8080 | API 문서 (OpenAPI 3.0) |
| **FastAPI (AI)** | 8000 | AI 서비스 API |
| **PostgreSQL** | 5432 | 메인 데이터베이스 |
| **Redis** | 6379 | 캐시 및 세션 |
| **Elasticsearch** | 9200 | 검색 엔진 HTTP |
| **Elasticsearch** | 9300 | 검색 엔진 Transport |
| **Neo4j Browser** | 7474 | 그래프 DB 웹 UI |
| **Neo4j Bolt** | 7687 | 그래프 DB 프로토콜 |
| **Kafka** | 9092 | Kafka 내부 통신 |
| **Kafka** | 9093 | Kafka 외부 통신 |
| **Kafka UI** | 8989 | Kafka 관리 UI |
| **Prometheus** | 9090 | 메트릭 수집 |
| **Grafana** | 3000 | 모니터링 대시보드 |

---

## 10. 프로젝트 특징

### 도메인
Yaldi는 **AI 기반 ERD 설계 및 협업 플랫폼**입니다.

**핵심 기능**:
- AI 기반 ERD 자동 생성 및 검증
- 실시간 협업 기능
- 버전 관리 및 스냅샷
- Mock 데이터 생성
- 다양한 데이터베이스 지원

### 핵심 기술적 특징

1. **Multi-Agent AI 시스템** (LangGraph)
   - GPT-4o 기반 도메인 분석 및 ERD 생성
   - 5개 전문 에이전트 협업

2. **Graph RAG** (Neo4j)
   - 지식 그래프 기반 컨텍스트 검색
   - APOC 및 Graph Data Science 플러그인 활용

3. **Vector Search** (pgvector)
   - 버전 임베딩 저장
   - 유사 ERD 검색

4. **Full-text Search** (Elasticsearch 8)
   - 프로젝트, ERD, 버전 검색
   - 한글 형태소 분석

5. **Real-time Collaboration**
   - WebSocket + STOMP 프로토콜 (백엔드)
   - STOMP.js + SockJS (프론트엔드)
   - Redis Pub/Sub 메시지 브로드캐스팅

6. **Event-driven Architecture** (Kafka)
   - 비동기 이벤트 처리
   - 서비스 간 느슨한 결합

7. **OAuth2 소셜 로그인**
   - Google, GitHub 연동
   - JWT 기반 세션 관리

8. **Clean Architecture + DDD**
   - 도메인 중심 설계
   - 계층 분리 (Domain, Infrastructure, Global)

9. **Observability**
   - Prometheus 메트릭 수집
   - Grafana 시각화
   - Spring Actuator 헬스체크

10. **분산 시스템 패턴**
    - Redisson 분산 락
    - ULID 분산 ID 생성
    - Kafka 이벤트 소싱

---

## 기술 스택 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│              Front-End (React 19 + TypeScript)               │
│    Vite + Tailwind CSS + Zustand + React Router DOM         │
│         STOMP.js + SockJS (WebSocket) + SSE                  │
└────────────────────────┬────────────────────────────────────┘
                         │ REST API / WebSocket / SSE
┌────────────────────────┴────────────────────────────────────┐
│                 Back-End (Spring Boot 3.5)                   │
│    ┌──────────┬──────────┬──────────┬──────────┬─────────┐  │
│    │   JPA    │  Redis   │   ES     │ WebSocket│  Kafka  │  │
│    └──────────┴──────────┴──────────┴──────────┴─────────┘  │
└─────┬───────┬─────────┬─────────┬──────────┬────────────────┘
      │       │         │         │          │
┌─────▼─┐ ┌──▼──┐ ┌────▼───┐ ┌──▼──┐   ┌───▼────┐
│Postgre│ │Redis│ │Elastic │ │Kafka│   │  AI    │
│  SQL  │ │     │ │ search │ │     │   │FastAPI │
│+vector│ └─────┘ └────────┘ └─────┘   └───┬────┘
└───────┘                                   │
                                       ┌────▼────┐
                                       │  Neo4j  │
                                       │ (Graph) │
                                       └─────────┘
```

---

## 버전 정보 요약

### Front-End
- **React**: 19.1.1
- **TypeScript**: 5.9.3
- **Vite**: 7.1.7
- **React Router DOM**: 7.9.4
- **Zustand**: 5.0.8
- **Tailwind CSS**: 3.4.18
- **Axios**: 1.12.2
- **@stomp/stompjs**: 7.2.1
- **SweetAlert2**: 11.26.3

### Back-End
- **Spring Boot**: 3.5.7
- **Java**: 21 (LTS)
- **PostgreSQL**: 16 (pgvector)
- **Redis**: 7-alpine
- **Elasticsearch**: 8.18.0
- **Kafka**: 3.8.1 (KRaft)
- **JWT**: 0.12.6
- **Redisson**: 3.27.2

### AI
- **Python**: 3.11-slim
- **FastAPI**: 0.109.0
- **OpenAI**: 1.10.0 (GPT-4o)
- **LangGraph**: 0.0.26+
- **Neo4j**: 5.15-community

---

## 프론트엔드 핵심 강점

- ⚡ **빠른 개발 경험**: Vite + React 19 + TypeScript
- 🔄 **실시간 협업**: WebSocket (STOMP) + SSE
- 🎨 **일관된 디자인**: Tailwind CSS 디자인 시스템
- 🛡️ **타입 안정성**: TypeScript strict 모드
- 🚀 **최적화된 번들링**: 코드 스플리팅 + 트리 쉐이킹
- 🔐 **안전한 인증**: 세션 기반 + 자동 에러 처리
- 📦 **경량 상태 관리**: Zustand + persist
- 🎯 **관심사 분리**: API, 비즈니스 로직, UI 레이어 명확한 분리

---

**최종 업데이트**: 2025-11-18
