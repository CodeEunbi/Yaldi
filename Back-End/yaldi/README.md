# Yaldi Backend

## 프로젝트 구조

### 아키텍처 레이어 구분

이 프로젝트는 클린 아키텍처와 DDD(Domain-Driven Design) 원칙을 기반으로 다음과 같은 3개의 레이어로 구성됩니다:

#### 1. Domain Layer (`com.yaldi.domain`)
비즈니스 로직과 도메인 모델을 포함하는 핵심 레이어입니다.

- **역할**: 비즈니스 규칙, 도메인 엔티티, 비즈니스 로직 처리
- **의존성**: 다른 레이어에 의존하지 않음 (순수 비즈니스 로직)
- **구성요소**:
  - `controller`: API 엔드포인트 정의
  - `service`: 비즈니스 로직 구현
  - `entity`: 도메인 엔티티 (JPA 엔티티)
  - `repository`: 데이터 접근 인터페이스
  - `dto`: 데이터 전송 객체

#### 2. Infrastructure Layer (`com.yaldi.infra`)
외부 시스템과의 통신 및 기술적 구현을 담당하는 레이어입니다.

- **역할**: 외부 서비스 연동, 기술적 구현체, 프레임워크 의존성 처리
- **의존성**: Domain Layer를 참조할 수 있음
- **구성요소**:
  - `security`: OAuth2, JWT 등 보안 관련 구현
  - `redis`: Redis 설정 및 구현
  - `websocket`: WebSocket 실시간 통신 구현

#### 3. Global Layer (`com.yaldi.global`)
애플리케이션 전역에서 사용되는 공통 설정 및 유틸리티를 제공하는 레이어입니다.

- **역할**: 공통 설정, 유틸리티, 전역 예외 처리, 공통 응답 구조
- **의존성**: 모든 레이어에서 참조 가능
- **구성요소**:
  - `config`: 전역 설정 (JPA, Jackson, WebMvc 등)
  - `common`: 공통 베이스 클래스 (엔티티, DTO 등)
  - `response`: 공통 응답 구조 및 상태 코드
  - `exception`: 전역 예외 처리
  - `util`: 유틸리티 클래스

### 폴더 구조

```
src/main/java/com/yaldi/
├── domain/                         # 도메인 레이어 (비즈니스 로직)
│   ├── agent/                      # AI Agent 도메인
│   │   ├── entity/                 # Agent 엔티티 (AgentRequest, AgentInteraction)
│   │   └── repository/             # Agent 리포지토리
│   ├── auth/                       # 인증 도메인
│   │   └── controller/             # 인증 API 컨트롤러
│   ├── comment/                    # 댓글 도메인
│   │   ├── entity/                 # 댓글 엔티티 (Comment, Reply)
│   │   └── repository/             # 댓글 리포지토리
│   ├── datamodel/                  # 데이터 모델 도메인
│   │   ├── entity/                 # 데이터 모델 엔티티 (DataModel, DataModelErdColumnRelation)
│   │   └── repository/             # 데이터 모델 리포지토리
│   ├── erd/                        # ERD 도메인
│   │   ├── entity/                 # ERD 엔티티 (ErdTable, ErdColumn, ErdRelation)
│   │   └── repository/             # ERD 리포지토리
│   ├── health/                     # 헬스체크 도메인
│   │   └── controller/             # 헬스체크 API 컨트롤러
│   ├── kafka/                      # Kafka 메시지 도메인
│   │   └── controller/             # Kafka 메시지 컨트롤러
│   ├── notification/               # 알림 도메인
│   │   ├── entity/                 # 알림 엔티티
│   │   └── repository/             # 알림 리포지토리
│   ├── project/                    # 프로젝트 도메인
│   │   ├── entity/                 # 프로젝트 엔티티 (Project, ProjectMemberRelation, ProjectMemberHistory)
│   │   └── repository/             # 프로젝트 리포지토리
│   ├── team/                       # 팀 도메인
│   │   ├── entity/                 # 팀 엔티티 (Team, UserTeamRelation, UserTeamHistory)
│   │   └── repository/             # 팀 리포지토리
│   ├── user/                       # 사용자 도메인
│   │   ├── controller/             # 사용자 API 컨트롤러
│   │   ├── dto/                    # 사용자 DTO
│   │   ├── entity/                 # 사용자 엔티티 (User, UserSocialAccount)
│   │   ├── repository/             # 사용자 리포지토리
│   │   └── service/                # 사용자 비즈니스 로직
│   └── version/                    # 버전 관리 도메인
│       ├── entity/                 # 버전 엔티티 (Version, Snapshot, EditHistory, MockData)
│       └── repository/             # 버전 리포지토리
│
├── infra/                          # 인프라 레이어 (기술적 구현)
│   ├── kafka/                      # Kafka 인프라
│   │   └── config/                 # Kafka 설정 (Producer, Consumer, Topic)
│   ├── redis/                      # Redis 인프라
│   │   └── config/                 # Redis 설정
│   ├── security/                   # 보안 인프라
│   │   ├── config/                 # Security 설정
│   │   ├── jwt/                    # JWT 처리
│   │   ├── oauth2/                 # OAuth2 처리
│   │   │   └── handler/            # OAuth2 핸들러
│   │   └── util/                   # Security 유틸리티
│   └── websocket/                  # WebSocket 인프라
│       ├── config/                 # WebSocket 설정
│       ├── controller/             # WebSocket 컨트롤러
│       ├── dto/                    # WebSocket DTO
│       └── service/                # WebSocket 서비스
│
└── global/                         # 글로벌 레이어 (공통 설정)
    ├── common/                     # 공통 베이스 클래스
    │   ├── BaseAuditEntity         # 생성/수정 시간 추적 엔티티
    │   ├── BaseCreateOnlyEntity    # 생성 시간만 추적하는 엔티티
    │   └── BaseSoftDeleteEntity    # 소프트 삭제 지원 엔티티
    ├── config/                     # 전역 설정
    │   ├── JacksonConfig           # JSON 직렬화 설정
    │   ├── JpaAuditingConfig       # JPA Auditing 설정
    │   └── WebMvcConfig            # WebMvc 설정
    ├── exception/                  # 예외 처리
    │   └── handler/                # 전역 예외 핸들러
    ├── response/                   # 공통 응답 구조
    │   ├── advice/                 # 응답 어드바이스
    │   └── status/                 # 응답 상태 코드
    └── util/                       # 유틸리티
```

### 레이어 간 의존성 규칙

```
┌─────────────────────┐
│   Domain Layer      │  ← 비즈니스 로직 (순수)
└─────────────────────┘
          ↑
          │ (참조 가능)
          │
┌─────────────────────┐
│ Infrastructure      │  ← 기술 구현
│     Layer           │
└─────────────────────┘
          ↑
          │ (참조 가능)
          │
┌─────────────────────┐
│   Global Layer      │  ← 공통 설정 (모든 레이어에서 사용)
└─────────────────────┘
```

**핵심 원칙**:
1. Domain Layer는 다른 레이어에 의존하지 않음
2. Infrastructure Layer는 Domain Layer를 참조 가능
3. Global Layer는 모든 레이어에서 참조 가능
4. 각 도메인은 독립적으로 개발 가능하도록 분리

### 도메인 모델

#### 핵심 도메인

**1. User Domain (사용자)**
- `User`: 사용자 기본 정보
- `UserSocialAccount`: 소셜 계정 연동 정보 (Google, GitHub, SSAFY)
- `Provider`: OAuth2 제공자 Enum

**2. Team Domain (팀)**
- `Team`: 팀 정보
- `UserTeamRelation`: 사용자-팀 관계
- `UserTeamHistory`: 팀 멤버 히스토리 (초대, 가입, 탈퇴 등)

**3. Project Domain (프로젝트)**
- `Project`: 프로젝트 정보
- `ProjectMemberRelation`: 프로젝트 멤버 관계 (OWNER, EDITOR, ADMIN)
- `ProjectMemberHistory`: 프로젝트 멤버 히스토리

**4. ERD Domain (ERD 설계)**
- `ErdTable`: ERD 테이블
- `ErdColumn`: ERD 컬럼
- `ErdRelation`: ERD 관계 (1:1, 1:N, N:M 등)

**5. Version Domain (버전 관리)**
- `Version`: 프로젝트 버전 (스키마 스냅샷)
- `Snapshot`: 사용자 생성 스냅샷
- `EditHistory`: 편집 히스토리
- `MockData`: Mock 데이터

**6. DataModel Domain (데이터 모델)**
- `DataModel`: 데이터 모델 (Entity, DTO)
- `DataModelErdColumnRelation`: 데이터 모델-ERD 컬럼 매핑

**7. Comment Domain (댓글)**
- `Comment`: 댓글
- `Reply`: 답글

**8. Notification Domain (알림)**
- `Notification`: 알림

**9. Agent Domain (AI Agent)**
- `AgentRequest`: AI Agent 요청
- `AgentInteraction`: AI Agent 상호작용

#### Entity 컨벤션

**Base Entity 클래스**
- `BaseAuditEntity`: created_at, updated_at (생성/수정 시간 추적)
- `BaseSoftDeleteEntity`: created_at, updated_at, deleted_at (소프트 삭제)
- `BaseCreateOnlyEntity`: created_at (생성 시간만 추적)

**PostgreSQL ENUM 타입 처리**
- 모든 ENUM은 Converter를 통해 PostgreSQL ENUM 타입과 매핑
- 예: `Provider`, `ProjectMemberRole`, `RelationType`, `DesignVerificationStatus` 등

**JSONB 타입 처리**
- Hypersistence Utils 라이브러리 사용
- 예: Version의 schemaData, EditHistory의 delta/beforeState/afterState

### 주요 기술 스택

- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **Database**: PostgreSQL (with vector extension)
- **Cache**: Redis
- **Authentication**: OAuth2, OIDC, JWT
- **Real-time**: WebSocket, Redis Pub/Sub
- **Message Queue**: Kafka (KRaft mode)
- **Monitoring**: Prometheus, Grafana, Kafka UI
- **Build Tool**: Gradle
- **ORM**: JPA/Hibernate with Hypersistence Utils (JSONB support)

### 개발 가이드

#### 새로운 도메인 추가 시

1. `domain/` 하위에 새로운 도메인 패키지 생성
2. 다음 구조로 구성:
   ```
   domain/{domain-name}/
   ├── controller/      # API 컨트롤러 (필요시)
   ├── service/         # 비즈니스 로직 (필요시)
   ├── entity/          # JPA 엔티티 (필수)
   ├── repository/      # Repository 인터페이스 (필수)
   └── dto/             # DTO (필요시)
   ```

3. **Entity 작성 규칙**:
   - 적절한 Base Entity 상속 (`BaseAuditEntity`, `BaseSoftDeleteEntity`, `BaseCreateOnlyEntity`)
   - PostgreSQL ENUM 타입 사용 시 Converter 구현
   - JSONB 타입 사용 시 `@Type(JsonBinaryType.class)` 어노테이션 사용
   - Soft Delete가 필요한 경우 `@SQLRestriction("deleted_at IS NULL")` 추가
   - Lombok annotations: `@Getter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

4. **Repository 작성 규칙**:
   - `JpaRepository<Entity, IdType>` 상속
   - 기본 CRUD 메서드는 상속으로 자동 제공
   - 커스텀 쿼리 메서드는 Spring Data JPA 메서드 네이밍 규칙 따르기
   - 복잡한 쿼리는 `@Query` 어노테이션 사용

5. **ENUM 작성 규칙** (PostgreSQL ENUM 타입 사용 시):
   ```java
   @Getter
   @RequiredArgsConstructor
   public enum YourEnum {
       VALUE1("VALUE1"),
       VALUE2("VALUE2");

       private final String value;
   }

   @Converter(autoApply = true)
   public class YourEnumConverter implements AttributeConverter<YourEnum, String> {
       // 구현 내용
   }
   ```

#### 새로운 인프라 기능 추가 시

1. `infra/` 하위에 새로운 인프라 패키지 생성
2. 설정과 구현을 분리하여 구성
3. Domain Layer에 의존하지 않도록 주의

#### 공통 기능 추가 시

1. 모든 레이어에서 사용되는 경우: `global/`에 추가
2. 특정 도메인에만 필요한 경우: 해당 `domain/` 패키지에 추가

### 데이터베이스 스키마

스키마 정의는 `src/main/resources/schema/` 디렉토리에서 관리합니다.

#### 스키마 파일 구조

```
src/main/resources/schema/
├── 00_init.sql          # Extension 및 ENUM 타입 정의
├── 01_ddl.sql           # 테이블 정의 (DDL)
├── 02_constraints.sql   # Foreign Key 제약조건
├── 03_indexes.sql       # 인덱스 정의
├── 04_triggers.sql      # 트리거 정의
└── 05_mock_data.sql     # 개발용 Mock 데이터
```

#### 주요 특징

**PostgreSQL Extensions**
- `vector`: 벡터 유사도 검색 지원 (AI 기능용)

**Custom ENUM Types**
- `design_verification_status_type`: QUEUED, RUNNING, SUCCESS, WARNING, FAILED, CANCELED
- `relation_type`: OPTIONAL_ONE_TO_ONE, STRICT_ONE_TO_ONE, OPTIONAL_ONE_TO_MANY, STRICT_ONE_TO_MANY, MANY_TO_MANY, SELF_RELATION
- `referential_action_type`: CASCADE, SET NULL, SET DEFAULT, RESTRICT, NO ACTION
- `project_member_role_type`: OWNER, EDITOR, ADMIN

**Soft Delete**
- 주요 엔티티는 `deleted_at` 컬럼을 통한 소프트 삭제 지원
- JPA에서 `@SQLRestriction("deleted_at IS NULL")`로 자동 필터링

**Timestamp**
- 모든 시간은 `TIMESTAMPTZ` (timezone-aware) 사용
- UTC 기준으로 저장, 클라이언트 타임존에 따라 응답 시 변환

### 실행 방법

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 실행
./gradlew bootRun
```

### Docker 환경

```bash
# Docker Compose로 실행
docker-compose up -d

# 스키마 변경시 볼륨을 삭제하고 다시 올려야 함
docker-compose down -v
```

### 서비스 포트 및 접속 정보

| 서비스 | 포트   | 접속 URL | 용도 |
|--------|------|----------|------|
| **Spring Boot** | `8080` | http://localhost:8080 | 메인 애플리케이션 API |
| **Swagger UI** | `8080` | http://localhost:8080/swagger | API 문서 |
| **PostgreSQL** | `5432` | localhost:5432 | 데이터베이스 |
| **Redis** | `6379` | localhost:6379 | 캐시 및 세션 |
| **Kafka** | `9092` | localhost:9092 | 메시지 브로커 |
| **Kafka UI** | `8989` | http://localhost:8989 | Kafka 메시지/토픽 모니터링 |
| **Prometheus** | `9090` | http://localhost:9090 | 메트릭 수집 |
| **Grafana** | `3000` | http://localhost:3000 | 메트릭 시각화 대시보드 |

#### 주요 엔드포인트

- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Swagger API Docs**: http://localhost:8080/swagger

#### 기본 계정 정보

- **Grafana**
  - Username: `admin`
  - Password: `admin`
  - 첫 로그인 시 비밀번호 변경 권장

#### Kafka 토픽 생성 예시

프로젝트에서 사용할 토픽은 `KafkaTopicConfig` 클래스에서 관리됩니다:
```java
// src/main/java/com/yaldi/infra/kafka/config/KafkaTopicConfig.java
@Bean
public NewTopic yourTopic() {
    return TopicBuilder.name("yaldi.your.topic")
            .partitions(3)
            .replicas(1)
            .build();
}
```

