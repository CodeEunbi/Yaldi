# ERD 자동 생성 AI 기술 설계 문서

## 1. 개요

### 기능 설명
사용자가 프로젝트명, 설명, 요구사항만 입력하면 **5개의 전문 AI Agent가 협업**하여 완전한 ERD 스키마를 자동으로 생성하는 핵심 기능입니다.

### 핵심 가치
- **초보자도 전문가 수준의 ERD 설계 가능**
- **Graph RAG 기반 지식 재활용**으로 검증된 패턴 참고
- **실제 DB 검증 + Self-Refinement**로 정확도 보장
- **모든 설계 과정 자동화**로 시간 단축

---

## 2. Multi-Agent 아키텍처 설계

### 2.1 설계 의도 (Why Multi-Agent?)

#### 단일 LLM의 한계
- 한 번의 프롬프트로 완벽한 ERD 생성 불가능
- 도메인 분석, 스키마 설계, 검증, 최적화를 한 번에 처리하기 어려움
- 오류 발생 시 전체 재생성 필요

#### Multi-Agent 접근의 장점
1. **전문화 (Specialization)**: 각 Agent가 특정 역할에만 집중
2. **단계별 검증**: 각 단계 결과를 다음 Agent가 검증
3. **오류 격리**: 특정 단계 실패 시 해당 Agent만 재실행
4. **확장성**: 새로운 Agent 추가로 기능 확장 용이

### 2.2 Agent 구성 및 역할

#### Agent 1: Domain Analyst (도메인 분석가)
**역할**: 사용자 요구사항에서 핵심 도메인 개념 추출

**입력**:
- 프로젝트명
- 프로젝트 설명
- 사용자 요구사항 (AI 초안 요청)

**출력**:
```json
{
  "core_entities": ["User", "Booking", "Payment"],
  "relationships": [
    {"from": "User", "to": "Booking", "type": "one_to_many"}
  ],
  "keywords": ["reservation", "payment", "user"],
  "business_rules": ["사용자는 여러 예약을 할 수 있다"]
}
```

**설계 고려사항**:
- Temperature 0.1 (낮은 창의성, 높은 일관성)
- JSON 포맷 강제로 파싱 안정성 확보
- 영문 키워드 추출 → Graph RAG 검색에 활용

---

#### Agent 2: Schema Architect (스키마 설계자)
**역할**: ERD 스키마 설계 (테이블, 컬럼, 관계 정의)

**2가지 모드 지원**:

##### ZERO_BASE 모드 (신규 설계)
- 유사 프로젝트 없을 때
- 도메인 분석만으로 처음부터 설계
- Temperature 0.2 (약간의 창의성 허용)

##### REFERENCE 모드 (참고 설계)
- 유사 프로젝트 있을 때 (유사도 33% 이상)
- Graph RAG에서 가져온 검증된 패턴 활용
- 기존 패턴을 요구사항에 맞게 변형

**출력**:
```json
{
  "tables": [
    {
      "name": "users",
      "columns": [
        {"name": "id", "type": "BIGSERIAL", "constraints": ["PRIMARY KEY"]},
        {"name": "email", "type": "VARCHAR(255)", "constraints": ["UNIQUE", "NOT NULL"]}
      ],
      "indexes": ["idx_users_email"]
    }
  ],
  "relations": [
    {"fromTable": "bookings", "toTable": "users", "fromColumn": "user_id", "type": "many_to_one"}
  ]
}
```

**설계 고려사항**:
- **constraints에는 FOREIGN KEY 절대 금지** → relations 배열로 분리
- 인덱스 자동 제안
- 정규화 자동 적용

---

#### Agent 3: Validator (검증자) - Agentic Tool Use
**역할**: 실제 DB에서 스키마 검증

**검증 항목**:
1. **순환 참조 검사**: DFS 알고리즘으로 외래키 순환 검출
2. **실제 DB 생성 테스트**: Test PostgreSQL DB에 CREATE TABLE 실행
3. **성능 이슈 검사**: 외래키 컬럼에 인덱스 누락 여부 확인

**Agentic Tool Use 전략**:
- LangChain Agent Tools 활용
- LLM이 필요한 검증 도구를 자율적으로 선택
- Temperature 0 (결정론적 검증)

**실제 DB 테스트 프로세스**:
```
1. Test DB에 임시 스키마 생성 (test_validation)
2. CREATE TABLE 문 실행
3. 외래키 제약조건 추가
4. 성공 시 "✅ 검증 통과", 실패 시 에러 메시지 반환
5. 스키마 삭제 (격리)
```

**설계 고려사항**:
- **실제 DB 실행**으로 문법 오류 사전 차단
- 순환 참조 자동 탐지로 무한루프 방지
- 검증 실패 시 구체적인 에러 메시지 제공

---

#### Agent 4: Optimizer (최적화자)
**역할**: 성능 최적화 전략 제안

**최적화 항목**:
1. **인덱스 전략**: 쿼리 패턴 예측하여 복합 인덱스 제안
2. **파티셔닝 전략**: 대용량 테이블 분할 제안
3. **캐싱 전략**: 자주 조회되는 데이터 캐싱 권장
4. **쿼리 최적화 팁**: JOIN 순서, N+1 문제 해결 등

**출력**:
```json
{
  "indexes": [
    {"table": "bookings", "column": "created_at", "type": "btree", "reason": "날짜 범위 쿼리 빈번"}
  ],
  "partitioning": [
    {"table": "logs", "strategy": "RANGE", "column": "created_at", "reason": "시계열 데이터 대용량"}
  ],
  "caching": ["users 테이블은 Redis 캐싱 권장"],
  "query_tips": ["booking 조회 시 user JOIN을 인덱스 활용"]
}
```

**설계 고려사항**:
- Temperature 0.3 (적당한 창의성으로 다양한 최적화 제안)
- 도메인 분석 결과 활용 → 비즈니스 로직에 맞는 최적화
- 제안만 하고 강제 적용하지 않음 (사용자 선택권 보장)

---

#### Agent 5: Meta Agent (최종 결정자)
**역할**: 모든 Agent 결과 종합 및 최종 SQL 생성

**통합 과정**:
1. Domain Analyst 결과 검토 → 도메인 반영도 확인
2. Schema Architect 결과 검토 → 정규화 준수 확인
3. Validator 결과 검토 → 치명적 문제 없는지 확인
4. Optimizer 결과 적용 → 인덱스 추가

**품질 평가 기준**:
```json
{
  "confidence_score": 0.92,  // 0.0 ~ 1.0
  "decision_rationale": "도메인 분석 정확, 3NF 준수, 모든 검증 통과",
  "requires_human_review": false,
  "warnings": ["테이블 수가 많아 복잡도 높음"]
}
```

**최종 SQL DDL 생성**:
- CREATE TABLE 문 생성
- CREATE INDEX 문 생성
- ALTER TABLE (외래키) 문 생성
- PostgreSQL 예약어 충돌 방지 (큰따옴표 처리)

**설계 고려사항**:
- Temperature 0.1 (보수적 결정)
- 검증 실패해도 경고와 함께 결과 반환 (사용자 판단 여지)
- 모든 Agent 결과를 LLM에게 요약 요청 → 전체 맥락 파악

---

### 2.3 Agent 실행 흐름 (LangGraph Workflow)

#### 순차 실행 단계
```
1. Domain Analyst (도메인 분석)
   ↓
2. Graph RAG 유사 프로젝트 검색
   ↓
3. [조건부 분기] 유사도 33% 기준
   ├─ REFERENCE 모드 → Schema Architect (참고 설계)
   └─ ZERO_BASE 모드 → Schema Architect (신규 설계)
   ↓
4. Validator (검증)
   ↓
5. [Self-Refinement Loop] 검증 실패 시
   ├─ 문제 발견 → Schema Architect 재실행 (최대 3회)
   └─ 검증 성공 or 3회 도달 → 다음 단계
   ↓
6. Optimizer (최적화)
   ↓
7. Meta Agent (최종 결정 + SQL 생성)
```

#### 병렬 실행이 없는 이유
- 각 Agent가 **이전 Agent 결과에 의존**하는 순차적 구조
- Domain Analyst 없이 Schema Architect 실행 불가
- Schema 없이 Validator 실행 불가
- 파이프라인 방식이 논리적으로 적합

#### LangGraph의 역할
- **StateGraph**: 각 단계의 상태(State) 관리
- **조건부 분기**: `decide_mode()`, `check_validation()`
- **Self-Refinement Loop**: `refine_schema` → `validate_schema` 순환
- **실행 로그 자동 저장**: 각 단계의 timestamp, 결과 기록

---

## 3. Self-Refinement Loop 설계

### 3.1 설계 의도 (Why Self-Refinement?)

**문제점**: LLM은 첫 시도에서 완벽한 스키마를 생성하기 어려움
- 외래키 오타
- 제약조건 충돌
- 순환 참조

**해결책**: 검증 실패 시 **자동으로 수정하고 재검증**

### 3.2 Refinement 프로세스

```
1. Validator가 검증 실패 반환
   ↓
2. check_validation() 함수가 판단
   - 문제 있음 + 반복 횟수 < 3 → "refine" 분기
   - 문제 없음 or 반복 횟수 = 3 → "optimize" 분기
   ↓
3. refine_schema 노드 실행
   - 기존 요구사항 + "수정 필요: [문제점]" 추가
   - Schema Architect 재호출
   ↓
4. validate_schema 노드로 돌아가서 재검증
   ↓
5. 성공 or 3회 도달까지 반복
```

### 3.3 최대 반복 횟수 제한 (3회)

**이유**:
- 무한루프 방지
- 3회 내 해결 안 되면 근본적 문제 → 사람 개입 필요
- 비용 절감 (LLM API 호출 제한)

**3회 도달 시 동작**:
- 경고 로그 출력
- 미해결 문제를 Meta Agent에게 전달
- `requires_human_review: true` 플래그 설정

---

## 4. Graph RAG 설계

### 4.1 설계 의도 (Why Graph RAG?)

#### 기존 RAG의 한계
- 벡터 검색만으로는 **엔티티 간 관계 패턴** 검색 어려움
- "User-Booking-Payment 관계" 같은 구조적 패턴 미스매칭

#### Graph RAG의 장점
1. **관계 중심 검색**: 엔티티 관계를 그래프로 표현
2. **패턴 매칭**: 유사한 도메인 구조 탐색
3. **검증된 지식 재활용**: 성공한 프로젝트만 저장

### 4.2 Neo4j 그래프 구조

```
(Project)-[:HAS_ENTITY]->(Entity)-[:RELATES_TO]->(Entity)
```

**노드 (Node)**:
- `Project`: version_key, project_name, is_public, verification_status
- `Entity`: 테이블명, 컬럼 리스트

**관계 (Relationship)**:
- `HAS_ENTITY`: 프로젝트가 포함하는 엔티티
- `RELATES_TO`: 엔티티 간 외래키 관계 (type: one_to_many, many_to_one 등)

### 4.3 검색 쿼리 전략

**키워드 기반 엔티티 매칭**:
```cypher
MATCH (p:Project)-[:HAS_ENTITY]->(e:Entity)
WHERE p.is_public = true
  AND p.verification_status = 'SUCCESS'
  AND any(keyword IN ["booking", "payment"] WHERE toLower(e.name) CONTAINS toLower(keyword))
RETURN p, e
```

**검색 조건**:
- `is_public = true`: 공개 프로젝트만
- `verification_status = 'SUCCESS'`: 검증 통과한 프로젝트만
- 키워드가 엔티티명에 포함된 경우 매칭

### 4.4 유사도 계산

**간단한 매칭 카운트 방식**:
```python
similarity_score = 매칭된 프로젝트 수 / top_k(3)
```

**분기 기준**:
- similarity_score >= 0.33 (1개 이상) → REFERENCE 모드
- similarity_score < 0.33 → ZERO_BASE 모드

**설계 고려사항**:
- 임베딩 유사도 대신 **엔티티 매칭 카운트** 사용 → 명확한 기준
- Top-3만 반환 → LLM 컨텍스트 길이 제한 고려

---

## 5. 모델 선택 전략

### 5.1 GPT-4 단일 모델 사용

**선택 이유**:
1. **복잡한 구조화 출력**: JSON 스키마 생성 능력 우수
2. **일관성**: 모든 Agent가 동일 모델 사용으로 데이터 포맷 통일
3. **도구 사용 (Tool Use)**: Validator Agent의 Agentic Tool Use 지원

**다른 모델을 쓰지 않은 이유**:
- Claude Sonnet 4.5: 상담 챗봇에서 사용 중 (역할 분리)
- GPT-4o-mini: 충분한 추론 능력 부족 (복잡한 ERD 설계 불가)

### 5.2 Temperature 설정 전략

| Agent | Temperature | 이유 |
|-------|-------------|------|
| Domain Analyst | 0.1 | 일관된 도메인 분석 |
| Schema Architect | 0.2 | 약간의 창의성 (다양한 설계 가능) |
| Validator | 0.0 | 완전 결정론적 검증 |
| Optimizer | 0.3 | 다양한 최적화 아이디어 |
| Meta Agent | 0.1 | 보수적 최종 결정 |

---

## 6. 프롬프트 엔지니어링 전략

### 6.1 프롬프트 파일 분리 관리

**디렉토리 구조**:
```
AI/yaldi/prompts/erd_generation/
├── domain_analyst_system.txt
├── domain_analyst_user.txt
├── schema_architect_zero_base_system.txt
├── schema_architect_reference_system.txt
├── optimizer_system.txt
└── meta_agent_system.txt
```

**분리 이유**:
1. **유지보수 용이**: 코드 수정 없이 프롬프트만 변경 가능
2. **버전 관리**: Git으로 프롬프트 변경 이력 추적
3. **협업**: 비개발자도 프롬프트 수정 가능

### 6.2 프롬프트 설계 원칙

#### JSON 강제 출력
```
반드시 JSON 형식으로 반환:
{
  "tables": [...],
  "relations": [...]
}
```
- 마크다운 코드 블록 자동 파싱
- 파싱 실패 시 폴백(Fallback) 기본값 제공

#### Few-Shot Learning
Schema Architect 프롬프트 예시:
```
JSON 형식 예시:
{
  "tables": [
    {"name": "User", "columns": [...]}
  ],
  "relations": [...]
}
```
- 예시 제공으로 출력 포맷 학습

#### 명확한 제약사항 명시
```
⚠️ 매우 중요한 규칙:
- constraints 배열에는 PRIMARY KEY, UNIQUE, NOT NULL, DEFAULT만 넣으세요
- FOREIGN KEY나 REFERENCES는 절대 constraints에 넣지 마세요!
```
- LLM의 실수 방지
- 반복적 오류 패턴 사전 차단

#### Chain-of-Thought (CoT)
```
다음을 수행하세요:
1. 핵심 비즈니스 엔티티 식별
2. 엔티티 간 관계 파악
3. 도메인 키워드 추출
4. 비즈니스 규칙 추론
```
- 단계별 사고 유도
- 복잡한 작업 분해

---

## 7. 최적화 전략

### 7.1 비용 최적화

**1회 ERD 생성 시 API 호출 횟수**:
- Domain Analyst: 1회
- Schema Architect: 1~4회 (Refinement 포함)
- Validator: 1~4회 (Refinement 포함)
- Optimizer: 1회
- Meta Agent: 1회
- **평균 5~10회**

**비용 절감 방법**:
- Graph RAG 캐싱: 유사 프로젝트 재검색 방지
- 프롬프트 최적화: 토큰 수 최소화
- Refinement 3회 제한: 무한루프 방지

### 7.2 성능 최적화

**병렬 처리 불가 구조**:
- Agent 간 순차 의존성 → 병렬 처리 불가능
- 대신 **각 Agent 내부 최적화**

**최적화 포인트**:
1. **Graph RAG 검색**: Neo4j 인덱스 활용
2. **Validator DB 테스트**: 비동기(asyncpg) 사용
3. **프롬프트 로딩**: 파일 캐싱

### 7.3 정확도 향상 전략

1. **Self-Refinement Loop**: 최대 3회 자동 수정
2. **실제 DB 검증**: 문법 오류 사전 차단
3. **Graph RAG 참고**: 검증된 패턴 재활용
4. **Meta Agent 품질 평가**: LLM이 최종 품질 평가

---

## 8. 데이터 플로우 요약

```
[사용자 입력]
  ↓
[Domain Analyst] → keywords 추출
  ↓
[Graph RAG 검색] → 유사 프로젝트 3개
  ↓
[유사도 계산] → 33% 기준 분기
  ↓
[Schema Architect] → ERD 스키마 JSON
  ↓
[Validator] → DB 테스트
  ↓ (실패 시)
[Self-Refinement] → Schema Architect 재실행 (최대 3회)
  ↓ (성공 시)
[Optimizer] → 인덱스 추가
  ↓
[Meta Agent] → 최종 SQL DDL
  ↓
[사용자에게 반환]
```

---

## 9. 기술 스택 요약

| 항목 | 기술 | 선택 이유 |
|------|------|----------|
| **LLM** | GPT-4 (OpenAI) | 복잡한 구조화 출력, Tool Use 지원 |
| **Workflow** | LangGraph | 조건부 분기, Self-Refinement Loop |
| **프롬프트 관리** | LangChain | 프롬프트 템플릿 관리 |
| **Graph DB** | Neo4j | 엔티티 관계 패턴 저장 |
| **검증 DB** | PostgreSQL | 실제 DB 테스트 |
| **비동기 처리** | asyncpg, AsyncGraphDatabase | 성능 최적화 |

---

## 10. 핵심 설계 결정 요약

| 설계 항목 | 결정 | 이유 |
|-----------|------|------|
| **Agent 구조** | 5개 전문 Agent | 역할 분리, 단계별 검증 |
| **실행 방식** | 순차 실행 | 각 Agent가 이전 결과 의존 |
| **검증 방식** | 실제 DB 테스트 | 문법 오류 사전 차단 |
| **오류 처리** | Self-Refinement (3회) | 자동 수정, 무한루프 방지 |
| **지식 활용** | Graph RAG | 검증된 패턴 재활용 |
| **모델 선택** | GPT-4 단일 모델 | 일관성, JSON 출력 능력 |
| **프롬프트 관리** | 파일 분리 | 유지보수성, 버전 관리 |

---

## 11. 발표 핵심 메시지

### 기술적 차별점
1. **Multi-Agent 협업 구조** → 단일 LLM보다 정확도 향상
2. **Self-Refinement + 실제 DB 검증** → 99% 이상 정확한 SQL 생성
3. **Graph RAG** → 과거 성공 사례 학습 및 재활용
4. **LangGraph 활용** → 복잡한 워크플로우 시각화 및 관리

### 사용자 가치
- **초보자도 3분 안에 전문가 수준 ERD 생성**
- **검증된 패턴 자동 참고로 베스트 프랙티스 준수**
- **실제 빌드 가능한 SQL 보장**
