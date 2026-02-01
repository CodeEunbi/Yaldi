# AI ERD 자동 생성 시스템

## 목차
1. [개요](#개요)
2. [아키텍처](#아키텍처)
3. [AI 기술 스택](#ai-기술-스택)
4. [전체 동작 흐름](#전체-동작-흐름)
5. [파일 구조 및 역할](#파일-구조-및-역할)
6. [API 명세](#api-명세)
7. [데이터 흐름](#데이터-흐름)

---

## 개요

### 기능 설명
사용자가 **3개의 간단한 텍스트 입력**만으로 AI가 자동으로 완성된 ERD 스키마를 생성해주는 시스템입니다.

### 사용자 입력
1. **프로젝트명** (필수): 예) "E-commerce Platform"
2. **프로젝트 설명** (선택): 예) "온라인 쇼핑몰 시스템"
3. **AI 초안 요청** (필수): 예) "사용자, 상품, 주문, 결제 기능이 필요합니다"

### 출력 결과
- **생성된 ERD 스키마** (tables, columns, relations)
- **SQL DDL 스크립트** (바로 실행 가능한 CREATE TABLE 문)
- **검증 리포트** (실제 PostgreSQL에서 테스트한 결과)
- **최적화 제안** (인덱스, 파티셔닝 등)
- **AI 사고 과정** (각 Agent의 판단 근거)
- **생성 모드**:
  - `REFERENCE` (유사도 70% 이상 → 기존 프로젝트 참고)
  - `ZERO_BASE` (유사도 70% 미만 → 신규 설계)

---

## 아키텍처

### 시스템 구성도

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Backend                         │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  ErdGenerationController                                    │    │
│  │         ↓                                                   │    │
│  │  ErdGenerationService                                       │    │
│  │         ↓                                                   │    │
│  │  ErdGenerationAiClient (WebClient)                         │    │
│  └─────────────────────────┬──────────────────────────────────┘    │
└────────────────────────────┼──────────────────────────────────────┘
                             │ HTTP POST /api/v1/erd/generate
                             ↓
┌─────────────────────────────────────────────────────────────────────┐
│                         AI Server (FastAPI)                         │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  FastAPI Endpoint (/api/v1/erd/generate)                   │    │
│  │         ↓                                                   │    │
│  │  ERDGenerationWorkflow (LangGraph)                         │    │
│  │         ↓                                                   │    │
│  │  ┌──────────────────────────────────────────────────────┐ │    │
│  │  │  1. DomainAnalyst (도메인 분석)                      │ │    │
│  │  │     - 엔티티 추출, 관계 파악, 키워드 생성           │ │    │
│  │  └──────────────────────────────────────────────────────┘ │    │
│  │         ↓                                                   │    │
│  │  ┌──────────────────────────────────────────────────────┐ │    │
│  │  │  Hybrid Search (Elasticsearch + Graph RAG)           │ │    │
│  │  │  - BM25 키워드 검색 + kNN 벡터 검색                 │ │    │
│  │  │  - Neo4j 엔티티 패턴 검색                           │ │    │
│  │  │  - 유사도 70% 기준으로 REFERENCE/ZERO_BASE 결정    │ │    │
│  │  └──────────────────────────────────────────────────────┘ │    │
│  │         ↓                                                   │    │
│  │  ┌──────────────────────────────────────────────────────┐ │    │
│  │  │  2. SchemaArchitect (스키마 설계)                    │ │    │
│  │  │     - REFERENCE 모드: 유사 프로젝트 참고 설계       │ │    │
│  │  │     - ZERO_BASE 모드: 처음부터 설계                 │ │    │
│  │  └──────────────────────────────────────────────────────┘ │    │
│  │         ↓                                                   │    │
│  │  ┌──────────────────────────────────────────────────────┐ │    │
│  │  │  3. Validator (검증) - Agentic Tool Use             │ │    │
│  │  │     - Test PostgreSQL에 실제로 테이블 생성 테스트   │ │    │
│  │  │     - 성공/실패 및 오류 메시지 반환                 │ │    │
│  │  └──────────────────────────────────────────────────────┘ │    │
│  │         ↓                                                   │    │
│  │  ┌──────────────────────────────────────────────────────┐ │    │
│  │  │  Self-Refinement Loop (최대 3회)                    │ │    │
│  │  │  - 검증 실패 시 SchemaArchitect가 수정              │ │    │
│  │  │  - 재검증 → 성공 시 다음 단계                       │ │    │
│  │  └──────────────────────────────────────────────────────┘ │    │
│  │         ↓                                                   │    │
│  │  ┌──────────────────────────────────────────────────────┐ │    │
│  │  │  4. Optimizer (최적화)                               │ │    │
│  │  │     - 인덱스, 파티셔닝, 캐싱 전략 제안              │ │    │
│  │  └──────────────────────────────────────────────────────┘ │    │
│  │         ↓                                                   │    │
│  │  ┌──────────────────────────────────────────────────────┐ │    │
│  │  │  5. MetaAgent (최종 결정)                            │ │    │
│  │  │     - 모든 Agent 결과 통합                           │ │    │
│  │  │     - SQL DDL 생성                                   │ │    │
│  │  │     - 최종 품질 평가 (confidence score)             │ │    │
│  │  └──────────────────────────────────────────────────────┘ │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        외부 시스템 (자동 학습)                      │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │  프로젝트 빌드 성공 시 (VersionProcessingConsumer)        │    │
│  │         ↓                                                   │    │
│  │  GraphRagAiClient → POST /api/v1/graph-rag/index          │    │
│  │         ↓                                                   │    │
│  │  Neo4j에 자동 인덱싱                                        │    │
│  │  - 엔티티 패턴 저장 (Project → Entity → Relations)        │    │
│  │  - 향후 REFERENCE 모드에서 AI가 참고                       │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

### 핵심 특징

1. **자가 학습 시스템**: 성공한 프로젝트를 Neo4j에 자동 저장 → AI가 점점 똑똑해짐
2. **실전 검증**: AI가 생성한 스키마를 실제 DB에서 테스트 (Agentic Tool Use)
3. **자동 개선**: 오류 발견 시 최대 3회까지 자동으로 수정 (Self-Refinement)
4. **적응형 설계**: 유사 프로젝트 있으면 참고, 없으면 신규 설계

---

## AI 기술 스택

### 1. Multi-Agent System (다중 에이전트 시스템)

5개의 전문화된 AI Agent가 협업하여 ERD를 생성합니다.

| Agent | 역할 | 출력 |
|-------|------|------|
| **DomainAnalyst** | 도메인 분석 | 핵심 엔티티, 관계, 비즈니스 룰, 검색 키워드 |
| **SchemaArchitect** | ERD 설계 | tables, columns, relations, constraints |
| **Validator** | 실행 검증 | 성공/실패, 오류 메시지, 수정 제안 |
| **Optimizer** | 성능 최적화 | 인덱스, 파티셔닝, 캐싱 전략 |
| **MetaAgent** | 통합 및 최종 결정 | SQL DDL, 최종 스키마, 품질 점수 |

**장점**:
- 각 Agent가 전문 분야에 집중 → 높은 품질
- Agent 간 피드백으로 오류 발견 및 수정
- 사고 과정이 투명하게 기록됨

### 2. Graph RAG (Neo4j)

**목적**: 엔티티 간 관계 패턴을 그래프로 저장하고 검색

**구조**:
```cypher
(Project {
  version_key: 123,
  project_name: "E-commerce",
  version_name: "v1.0.0",
  is_public: true,
  verification_status: "SUCCESS"
})
  -[:HAS_ENTITY]->
(Entity {name: "User", columns: ["id", "email", "password"]})
  -[:RELATES_TO {type: "ONE_TO_MANY"}]->
(Entity {name: "Order"})
```

**활용**:
- 사용자 요청에서 추출한 키워드로 유사한 엔티티 패턴 검색
- 예: "사용자, 주문" → User-Order 관계를 가진 프로젝트들 찾기
- REFERENCE 모드에서 이 패턴들을 참고하여 설계

**자동 학습**:
- 프로젝트 검증 성공 시 자동으로 Neo4j에 저장
- 시간이 지날수록 참고할 패턴이 늘어남 → AI가 똑똑해짐

### 3. Hybrid RAG (Elasticsearch)

**목적**: 유사한 프로젝트를 빠르고 정확하게 찾기

**구성**:
1. **BM25 키워드 검색**: 텍스트 매칭 (예: "쇼핑몰", "주문")
2. **kNN 벡터 검색**: 의미적 유사도 (OpenAI Embeddings)
3. **RRF (Reciprocal Rank Fusion)**: 두 결과를 통합하여 최종 순위 결정

**필터링**:
```python
filter = {
    "is_public": True,
    "design_verification_status": "SUCCESS"
}
```
- 공개 프로젝트만
- 검증 통과한 프로젝트만

**유사도 계산**:
- 70% 이상: REFERENCE 모드 (유사 프로젝트 참고)
- 70% 미만: ZERO_BASE 모드 (신규 설계)

### 4. LangGraph Workflow

**목적**: 복잡한 Multi-Agent 워크플로우 오케스트레이션

**주요 기능**:
- **State Management**: Agent 간 데이터 공유
- **Conditional Branching**: 유사도에 따라 REFERENCE/ZERO_BASE 분기
- **Loops**: 검증 실패 시 최대 3회까지 재시도
- **Sequential Flow**: Agent들이 순차적으로 실행

**워크플로우 그래프**:
```
START
  ↓
DomainAnalyst
  ↓
HybridSearch (Elasticsearch + Neo4j)
  ↓
[유사도 >= 0.7?]
  ├─ YES → REFERENCE Mode (유사 프로젝트 참고)
  └─ NO  → ZERO_BASE Mode (신규 설계)
  ↓
SchemaArchitect
  ↓
Validator (실제 DB 테스트)
  ↓
[검증 성공?]
  ├─ NO  → SchemaArchitect (수정) → Validator (최대 3회)
  └─ YES → 다음 단계
  ↓
Optimizer
  ↓
MetaAgent
  ↓
END (최종 결과 반환)
```

### 5. Agentic Tool Use (도구 사용 에이전트)

**개념**: AI가 외부 도구를 직접 사용하여 작업 수행

**구현 예시 (Validator Agent)**:
```python
async def validate_schema(self, schema):
    # AI가 생성한 스키마를 실제 PostgreSQL에서 테스트
    conn = await asyncpg.connect(TEST_POSTGRES_URL)

    try:
        for table in schema['tables']:
            # 실제로 CREATE TABLE 실행
            await conn.execute(table['sql'])

        return {"success": True, "message": "검증 성공"}
    except Exception as e:
        return {"success": False, "errors": [str(e)]}
```

**장점**:
- 이론이 아닌 실제 실행 결과로 검증
- 오류를 정확하게 파악하여 수정 가능

### 6. Self-Refinement Loop (자가 개선 루프)

**동작 원리**:
```
1회차: SchemaArchitect → Validator → 실패 (외래키 오류)
       ↓
2회차: SchemaArchitect (오류 수정) → Validator → 실패 (NULL 제약 오류)
       ↓
3회차: SchemaArchitect (추가 수정) → Validator → 성공!
```

**최대 3회 제한**:
- 무한 루프 방지
- 3회 내에 해결 못하면 현재 상태로 반환 (품질 점수 낮음)

**학습 효과**:
- 각 반복마다 AI가 이전 오류를 학습
- 점진적으로 개선된 스키마 생성

---

## 전체 동작 흐름

### Phase 1: 요청 수신 및 도메인 분석

```
[사용자]
  → POST /api/erd-generation/generate
  → ErdGenerationController
  → ErdGenerationService
  → ErdGenerationAiClient (WebClient)

[AI 서버]
  → FastAPI Endpoint
  → ERDGenerationWorkflow.ainvoke()
  → DomainAnalystAgent.analyze()

출력:
{
  "core_entities": ["User", "Product", "Order", "Payment"],
  "relationships": ["User-Order: ONE_TO_MANY", "Order-Product: MANY_TO_MANY"],
  "keywords": ["user", "product", "order", "payment", "ecommerce"],
  "business_rules": ["주문은 하나의 사용자에게만 속함", "상품은 여러 주문에 포함 가능"]
}
```

### Phase 2: 유사 프로젝트 검색 (Hybrid Search)

```
1. Elasticsearch 검색:
   - BM25 키워드: "user", "product", "order"
   - kNN 벡터: 의미적 유사도 계산
   - 필터: is_public=true, design_verification_status=SUCCESS

2. Neo4j Graph 검색:
   - MATCH (p:Project)-[:HAS_ENTITY]->(e:Entity)
   - WHERE e.name IN ["User", "Product", "Order"]
   - 엔티티 패턴이 유사한 프로젝트 찾기

3. 유사도 계산:
   - RRF (Reciprocal Rank Fusion)로 두 결과 통합
   - 최종 유사도 점수: 0.0 ~ 1.0

4. 모드 결정:
   - 유사도 >= 0.7 → REFERENCE 모드 (Top 5 프로젝트 참고)
   - 유사도 < 0.7  → ZERO_BASE 모드 (신규 설계)
```

### Phase 3: ERD 스키마 설계

```
SchemaArchitectAgent.design_schema()

[REFERENCE 모드]
  - 유사 프로젝트들의 스키마 분석
  - 공통 패턴 추출 (예: User-Order 관계)
  - 사용자 요구사항에 맞게 커스터마이징
  - 베스트 프랙티스 적용

[ZERO_BASE 모드]
  - 도메인 분석 결과만으로 설계
  - 정규화 원칙 적용 (1NF, 2NF, 3NF)
  - 일반적인 데이터베이스 설계 패턴 사용

출력:
{
  "tables": [
    {
      "name": "users",
      "columns": [
        {"name": "id", "type": "SERIAL", "constraints": ["PRIMARY KEY"]},
        {"name": "email", "type": "VARCHAR(255)", "constraints": ["UNIQUE", "NOT NULL"]}
      ]
    }
  ],
  "relations": [
    {"fromTable": "users", "toTable": "orders", "type": "ONE_TO_MANY"}
  ]
}
```

### Phase 4: 검증 및 자가 개선 (최대 3회)

```
iteration = 1

LOOP:
  ValidatorAgent.validate()
    → Test PostgreSQL 연결
    → CREATE TABLE 실행

  [성공]
    → BREAK (다음 단계로)

  [실패]
    → 오류 메시지 수집
    → SchemaArchitect에게 피드백
    → iteration++
    → if iteration > 3: BREAK (현재 상태로 반환)
    → CONTINUE LOOP

예시:
1회차: 외래키 타입 불일치 오류
2회차: NOT NULL 제약 위반
3회차: 성공! → 다음 단계
```

### Phase 5: 최적화 및 최종 결정

```
OptimizerAgent.optimize()
  → 인덱스 제안: "users.email에 UNIQUE INDEX"
  → 파티셔닝 제안: "orders 테이블을 created_at으로 월별 파티셔닝"
  → 캐싱 전략: "상품 목록은 Redis 캐싱 권장"

MetaAgent.orchestrate()
  → 모든 Agent 결과 통합
  → SQL DDL 생성:
      CREATE TABLE users (...);
      CREATE TABLE orders (...);
      ALTER TABLE orders ADD CONSTRAINT fk_user ...;
  → 품질 평가 (confidence_score: 0.0 ~ 1.0)
  → 최종 응답 생성
```

### Phase 6: 결과 반환 및 자동 학습

```
[사용자에게 반환]
{
  "mode": "REFERENCE",
  "similarity_score": 0.85,
  "generated_schema": {...},
  "sql_script": "CREATE TABLE ...",
  "validation_report": {"success": true, "errors": []},
  "optimization_suggestions": {...},
  "confidence_score": 0.92,
  "execution_time_ms": 12500
}

[자동 학습 - 백그라운드]
만약 이 스키마로 프로젝트를 만들고 나중에 빌드가 성공하면:
  → VersionProcessingConsumer가 감지
  → GraphRagAiClient.indexToGraph() 호출
  → Neo4j에 엔티티 패턴 저장
  → 다음에 유사한 요청이 오면 이 프로젝트를 참고 가능
```

---

## 파일 구조 및 역할

### Spring Boot Backend

#### 1. Controller
**파일**: `Back-End/yaldi/src/main/java/com/yaldi/domain/erd_generation/controller/ErdGenerationController.java`

```java
@PostMapping("/generate")
public ResponseEntity<ApiResponse<ErdGenerationResponse>> generateErd(
    @Valid @RequestBody ErdGenerationRequest request
)
```

**역할**:
- REST API 엔드포인트 제공
- 요청 검증 (Jakarta Validation)
- Service 계층 호출
- 응답 반환

#### 2. Service
**파일**: `Back-End/yaldi/src/main/java/com/yaldi/domain/erd_generation/service/ErdGenerationService.java`

**역할**:
- 비즈니스 로직 처리
- AI Client 호출
- 로깅 및 에러 핸들링

#### 3. AI Client
**파일**: `Back-End/yaldi/src/main/java/com/yaldi/domain/erd_generation/client/ErdGenerationAiClient.java`

**역할**:
- WebClient로 AI 서버 통신
- HTTP 요청/응답 처리
- 타임아웃 관리 (기본 30초)

```java
public ErdGenerationResponse generateErd(
    String projectName,
    String projectDescription,
    String userPrompt
)
```

#### 4. DTO
**요청**: `Back-End/yaldi/src/main/java/com/yaldi/domain/erd_generation/dto/request/ErdGenerationRequest.java`

```java
public record ErdGenerationRequest(
    @NotBlank String projectName,
    String projectDescription,
    @NotBlank @Size(min=10, max=2000) String userPrompt
)
```

**응답**: `Back-End/yaldi/src/main/java/com/yaldi/domain/erd_generation/dto/response/ErdGenerationResponse.java`

```java
public class ErdGenerationResponse {
    private String mode;                            // REFERENCE / ZERO_BASE
    private Double similarityScore;                 // 0.0 ~ 1.0
    private List<Map<String, Object>> similarProjects;
    private Map<String, Object> generatedSchema;
    private String sqlScript;
    private String explanation;
    private List<AgentThought> agentThoughts;       // AI 사고 과정
    private Map<String, Object> validationReport;
    private Map<String, Object> optimizationSuggestions;
    private Integer executionTimeMs;
    private Double confidenceScore;                 // 0.0 ~ 1.0
}
```

#### 5. Graph RAG Client (자동 학습용)
**파일**: `Back-End/yaldi/src/main/java/com/yaldi/domain/version/client/GraphRagAiClient.java`

**역할**:
- 빌드 성공한 프로젝트를 Neo4j에 인덱싱
- AI가 향후 참고할 수 있도록 지식 축적

```java
public boolean indexToGraph(
    Long versionKey,
    String versionName,
    String versionDescription,
    String projectName,
    String projectDescription,
    Map<String, Object> schemaData,
    Boolean isPublic,
    String designVerificationStatus
)
```

#### 6. Kafka Consumer (자동 학습 트리거)
**파일**: `Back-End/yaldi/src/main/java/com/yaldi/domain/version/listener/VersionProcessingConsumerListener.java`

**역할**:
- 프로젝트 빌드 검증 완료 시 실행
- 검증 성공(SUCCESS) 시 Neo4j에 자동 인덱싱

```java
// Graph RAG 인덱싱 (검증 성공 시에만)
if (verificationSuccess && version.getDesignVerificationStatus() == DesignVerificationStatus.SUCCESS) {
    graphRagAiClient.indexToGraph(...);
}
```

---

### AI Server (FastAPI)

#### 1. API Endpoint
**파일**: `ai/yaldi/api/v1/erd_generation.py`

```python
@router.post("/generate", response_model=ERDGenerationResponse)
async def generate_erd(request: ERDGenerationRequest):
    workflow = await get_erd_workflow()
    result = await workflow.ainvoke({
        "project_name": request.project_name,
        "user_requirements": {
            "project_description": request.project_description,
            "user_prompt": request.user_prompt
        }
    })
    return result
```

**역할**:
- FastAPI 엔드포인트
- Pydantic으로 요청/응답 검증
- LangGraph 워크플로우 실행

#### 2. LangGraph Workflow
**파일**: `ai/yaldi/workflows/erd_workflow.py`

**역할**:
- Agent들을 순차적으로 오케스트레이션
- Conditional branching (REFERENCE/ZERO_BASE)
- Self-refinement loop 관리
- State 관리 (Agent 간 데이터 공유)

```python
class ERDGenerationWorkflow:
    def build_graph(self):
        workflow = StateGraph(ERDGenerationState)

        # Agent 노드 추가
        workflow.add_node("domain_analysis", self.domain_analysis_node)
        workflow.add_node("hybrid_search", self.hybrid_search_node)
        workflow.add_node("schema_design", self.schema_design_node)
        workflow.add_node("validation", self.validation_node)
        workflow.add_node("refinement", self.refinement_node)
        workflow.add_node("optimization", self.optimization_node)
        workflow.add_node("final_decision", self.final_decision_node)

        # Conditional edge (REFERENCE / ZERO_BASE)
        workflow.add_conditional_edges(
            "hybrid_search",
            self.decide_mode,
            {
                "reference": "schema_design",
                "zero_base": "schema_design"
            }
        )

        # Refinement loop
        workflow.add_conditional_edges(
            "validation",
            self.should_refine,
            {
                "refine": "refinement",
                "continue": "optimization"
            }
        )
```

#### 3. Agents (Multi-Agent System)

**DomainAnalyst**: `ai/yaldi/agents/erd_generation/domain_analyst.py`
```python
class DomainAnalystAgent:
    async def analyze(self, project_name, description, user_prompt):
        # LLM 호출 (GPT-4)
        result = await self.llm.ainvoke(prompt)

        return {
            "core_entities": [...],
            "relationships": [...],
            "keywords": [...],
            "business_rules": [...]
        }
```

**SchemaArchitect**: `ai/yaldi/agents/erd_generation/schema_architect.py`
```python
class SchemaArchitectAgent:
    async def design_schema(self, domain_analysis, reference_projects=None):
        if reference_projects:
            return await self._design_from_reference(...)
        else:
            return await self._design_from_scratch(...)

        return {
            "tables": [...],
            "relations": [...],
            "constraints": [...]
        }
```

**Validator**: `ai/yaldi/agents/erd_generation/validator_agent.py`
```python
class ValidatorAgent:
    async def validate(self, schema):
        # Agentic Tool Use: 실제 DB 테스트
        test_result = await self._test_schema_creation_async(schema)

        return {
            "success": True/False,
            "errors": [...],
            "suggestions": [...]
        }

    async def _test_schema_creation_async(self, schema):
        conn = await asyncpg.connect(TEST_POSTGRES_URL)
        try:
            for table in schema['tables']:
                await conn.execute(table['sql'])
            return {"success": True}
        except Exception as e:
            return {"success": False, "error": str(e)}
```

**Optimizer**: `ai/yaldi/agents/erd_generation/optimizer_agent.py`
```python
class OptimizerAgent:
    async def optimize(self, schema, validation_result):
        return {
            "index_suggestions": [...],
            "partitioning_suggestions": [...],
            "caching_strategy": [...]
        }
```

**MetaAgent**: `ai/yaldi/agents/erd_generation/meta_agent.py`
```python
class MetaAgent:
    async def orchestrate(self, all_agent_results):
        # SQL DDL 생성
        sql_script = self._generate_sql_script(schema)

        # 품질 평가
        confidence_score = self._evaluate_quality(all_results)

        return {
            "sql_script": sql_script,
            "confidence_score": confidence_score,
            "explanation": "..."
        }
```

#### 4. RAG Systems

**Graph RAG**: `ai/yaldi/rag/graph_rag.py`

```python
class GraphRAG:
    async def index_project_schema(
        self, version_key, version_name, project_name, schema_data, ...
    ):
        # Neo4j에 저장
        await session.run("""
            MERGE (p:Project {version_key: $version_key})
            SET p.project_name = $project_name, ...
        """)

        # 엔티티 노드 생성
        for table in schema_data['tables']:
            await session.run("""
                MERGE (e:Entity {name: $name})
                MERGE (p)-[:HAS_ENTITY]->(e)
            """)

        # 관계 생성
        for relation in schema_data['relations']:
            await session.run("""
                MATCH (from:Entity {name: $from})
                MATCH (to:Entity {name: $to})
                MERGE (from)-[:RELATES_TO {type: $type}]->(to)
            """)

    async def search_similar_patterns(self, keywords, top_k=5):
        # 키워드로 유사 엔티티 패턴 검색
        result = await session.run("""
            MATCH (p:Project)-[:HAS_ENTITY]->(e:Entity)
            WHERE e.name IN $keywords
            RETURN p, collect(e) as entities
        """)
```

**Hybrid Search**: `ai/yaldi/rag/hybrid_search.py`

```python
class HybridSearch:
    async def search_similar_projects(self, keywords, embedding, top_k=5):
        # 1. BM25 키워드 검색
        bm25_results = await self.es.search(
            query={"match": {"description": " ".join(keywords)}}
        )

        # 2. kNN 벡터 검색
        knn_results = await self.es.search(
            knn={"field": "vector", "query_vector": embedding}
        )

        # 3. RRF (Reciprocal Rank Fusion)
        final_results = self._rrf_fusion(bm25_results, knn_results)

        return final_results[:top_k]
```

#### 5. Graph RAG Indexing API
**파일**: `ai/yaldi/api/v1/graph_indexing.py`

```python
@router.post("/index")
async def index_to_graph(request: GraphIndexingRequest):
    graph_rag = await get_graph_rag()

    await graph_rag.index_project_schema(
        version_key=request.version_key,
        version_name=request.version_name,
        project_name=request.project_name,
        schema_data=request.schema_data,
        ...
    )

    return {"success": True, "message": "인덱싱 완료"}
```

**역할**:
- Spring Boot에서 호출하는 인덱싱 전용 API
- 빌드 성공한 프로젝트를 Neo4j에 저장

---

## API 명세

### 1. ERD 자동 생성

**Endpoint**: `POST /api/erd-generation/generate`

**Request**:
```json
{
  "projectName": "E-commerce Platform",
  "projectDescription": "온라인 쇼핑몰 시스템",
  "userPrompt": "사용자, 상품, 주문, 결제, 배송 기능이 필요합니다. 사용자는 여러 주문을 할 수 있고, 주문은 여러 상품을 포함할 수 있습니다."
}
```

**Response**:
```json
{
  "isSuccess": true,
  "code": "OK",
  "message": "성공",
  "result": {
    "mode": "REFERENCE",
    "similarityScore": 0.85,
    "similarProjects": [
      {
        "versionKey": 123,
        "projectName": "Shopping Mall",
        "similarity": 0.87
      }
    ],
    "generatedSchema": {
      "tables": [
        {
          "name": "users",
          "columns": [
            {
              "name": "id",
              "type": "SERIAL",
              "constraints": ["PRIMARY KEY"]
            },
            {
              "name": "email",
              "type": "VARCHAR(255)",
              "constraints": ["UNIQUE", "NOT NULL"]
            }
          ]
        }
      ],
      "relations": [
        {
          "fromTable": "users",
          "toTable": "orders",
          "type": "ONE_TO_MANY",
          "foreignKey": "user_id"
        }
      ]
    },
    "sqlScript": "CREATE TABLE users (\n  id SERIAL PRIMARY KEY,\n  email VARCHAR(255) UNIQUE NOT NULL\n);\n\nCREATE TABLE orders (\n  id SERIAL PRIMARY KEY,\n  user_id INTEGER REFERENCES users(id)\n);",
    "explanation": "유사한 E-commerce 프로젝트를 참고하여 설계했습니다. User와 Order는 1:N 관계이며...",
    "agentThoughts": [
      {
        "step": "DomainAnalysis",
        "timestamp": "2025-01-15T10:30:00Z",
        "result": "5개의 핵심 엔티티 추출: User, Product, Order, Payment, Delivery"
      },
      {
        "step": "HybridSearch",
        "timestamp": "2025-01-15T10:30:02Z",
        "result": "유사도 85%의 프로젝트 3개 발견 → REFERENCE 모드 선택"
      }
    ],
    "validationReport": {
      "success": true,
      "message": "모든 테이블이 성공적으로 생성되었습니다",
      "errors": [],
      "warnings": []
    },
    "optimizationSuggestions": {
      "indexes": [
        "users.email에 UNIQUE INDEX 추가 (이미 적용됨)",
        "orders.created_at에 INDEX 추가 (조회 성능 향상)"
      ],
      "partitioning": [
        "orders 테이블을 created_at 기준으로 월별 파티셔닝 권장"
      ],
      "caching": [
        "상품 목록은 Redis 캐싱 권장 (읽기가 많은 데이터)"
      ]
    },
    "executionTimeMs": 12500,
    "confidenceScore": 0.92
  }
}
```

### 2. Graph RAG 인덱싱 (내부 API)

**Endpoint**: `POST /api/v1/graph-rag/index`

**Request**:
```json
{
  "version_key": 123,
  "version_name": "v1.0.0",
  "version_description": "초기 릴리즈",
  "project_name": "E-commerce",
  "project_description": "온라인 쇼핑몰",
  "schema_data": {
    "tables": [...],
    "relations": [...]
  },
  "is_public": true,
  "design_verification_status": "SUCCESS"
}
```

**Response**:
```json
{
  "success": true,
  "message": "프로젝트가 Neo4j에 성공적으로 인덱싱되었습니다",
  "version_key": 123
}
```

---

## 데이터 흐름

### 1. ERD 생성 요청 흐름

```
[사용자]
  ↓ (3개 텍스트 입력)
[Frontend]
  ↓ POST /api/erd-generation/generate
[Spring Boot - ErdGenerationController]
  ↓ generateErd()
[Spring Boot - ErdGenerationService]
  ↓ erdGenerationAiClient.generateErd()
[Spring Boot - ErdGenerationAiClient (WebClient)]
  ↓ HTTP POST
[AI Server - FastAPI Endpoint]
  ↓ POST /api/v1/erd/generate
[AI Server - ERDGenerationWorkflow]
  ↓ workflow.ainvoke()
[Multi-Agent System]
  ↓
  1. DomainAnalyst → 도메인 분석
  2. HybridSearch → 유사 프로젝트 검색
  3. SchemaArchitect → ERD 설계
  4. Validator → 실제 DB 테스트
  5. Refinement Loop (최대 3회)
  6. Optimizer → 최적화 제안
  7. MetaAgent → 최종 결정
  ↓
[AI Server - Response]
  ↓ JSON 응답
[Spring Boot - ErdGenerationResponse]
  ↓ ApiResponse 래핑
[Frontend]
  ↓
[사용자에게 표시]
```

### 2. 자동 학습 흐름 (백그라운드)

```
[프로젝트 빌드]
  ↓
[빌드 검증 완료]
  ↓ Kafka Message
[VersionProcessingConsumerListener]
  ↓ 검증 결과 확인
[designVerificationStatus == SUCCESS?]
  ↓ YES
[GraphRagAiClient.indexToGraph()]
  ↓ HTTP POST /api/v1/graph-rag/index
[AI Server - Graph Indexing API]
  ↓
[GraphRAG.index_project_schema()]
  ↓
[Neo4j 저장]
  - (Project) 노드 생성
  - (Entity) 노드들 생성
  - [:HAS_ENTITY], [:RELATES_TO] 관계 생성
  ↓
[향후 ERD 생성 시 참고 가능]
```

### 3. 데이터 저장소별 역할

| 저장소 | 용도 | 데이터 |
|--------|------|--------|
| **PostgreSQL** | 실제 프로젝트 데이터 | 프로젝트, 버전, 스키마, 검증 결과 |
| **Elasticsearch** | 검색 및 유사도 계산 | 프로젝트 메타데이터, 벡터 임베딩 |
| **Neo4j** | 엔티티 관계 패턴 | 엔티티 노드, 관계 엣지 |
| **Test PostgreSQL** | AI 검증용 | 임시 테이블 (검증 후 삭제) |

---

## 성능 최적화

### 1. 캐싱 전략

**생성 결과 캐싱**:
- 동일한 요청은 Redis에 캐싱 (24시간)
- Cache Key: `erd:generation:{hash(projectName+userPrompt)}`

**유사 프로젝트 캐싱**:
- Elasticsearch 쿼리 결과 캐싱 (1시간)
- 자주 검색되는 패턴 우선 캐싱

### 2. 비동기 처리

**Non-blocking I/O**:
- Spring Boot: WebClient (Reactive)
- AI Server: async/await (Python asyncio)
- 모든 Agent가 비동기로 실행

**병렬 처리**:
```python
# Elasticsearch와 Neo4j 검색을 동시에 실행
es_task = asyncio.create_task(elasticsearch_search())
neo4j_task = asyncio.create_task(neo4j_search())

es_results, neo4j_results = await asyncio.gather(es_task, neo4j_task)
```

### 3. 타임아웃 관리

```java
// Spring Boot
@Value("${ai.server.timeout:30000}")  // 30초
private long timeout;

webClient.post()
    .retrieve()
    .bodyToMono(Response.class)
    .block(Duration.ofMillis(timeout));
```

```python
# AI Server
GENERATION_TIMEOUT = 120  # 2분
```

---

## 모니터링 및 로깅

### 1. 로깅 포인트

**Spring Boot**:
```java
log.info("ERD 생성 요청 - Project: {}", projectName);
log.info("ERD 생성 완료 - Mode: {}, Confidence: {}", mode, confidence);
log.error("ERD 생성 실패 - Project: {}", projectName, e);
```

**AI Server**:
```python
logger.info(f"Domain analysis started - project: {project_name}")
logger.info(f"Hybrid search found {len(results)} similar projects")
logger.error(f"Validation failed: {error_message}")
```

### 2. 메트릭 수집

- 생성 시간 (execution_time_ms)
- 모드 분포 (REFERENCE vs ZERO_BASE)
- 유사도 분포
- 검증 성공률
- Refinement 반복 횟수
- Confidence Score 분포

---
