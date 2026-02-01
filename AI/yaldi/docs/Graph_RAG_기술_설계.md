# Graph RAG 기술 설계

## 목차
1. [Graph RAG란?](#1-graph-rag란)
2. [왜 Graph RAG를 사용하나?](#2-왜-graph-rag를-사용하나)
3. [Graph RAG 구조](#3-graph-rag-구조)
4. [데이터 저장 흐름](#4-데이터-저장-흐름)
5. [검색 흐름](#5-검색-흐름)
6. [설계 결정](#6-설계-결정)

---

## 1. Graph RAG란?

### 1.1 RAG (Retrieval Augmented Generation)

**RAG = 검색 증강 생성**

AI가 답변을 생성할 때, **외부 지식을 검색하여 참고**하는 기법입니다.

**일반 LLM의 한계:**
```
질문: "우리 회사 2023년 매출이 얼마야?"
LLM: "저는 귀사의 내부 데이터를 모릅니다." ❌
```

**RAG 적용:**
```
질문: "우리 회사 2023년 매출이 얼마야?"
↓
[RAG] 회사 DB에서 "2023년 매출" 검색
→ 결과: 150억원
↓
LLM: "귀사의 2023년 매출은 150억원입니다." ✅
```

### 1.2 Graph RAG란?

**Graph RAG = 그래프 DB를 활용한 RAG**

- 일반 RAG: 벡터 검색, 키워드 검색 사용
- **Graph RAG**: **관계 그래프**를 활용한 검색

**그래프의 장점:**
- 엔티티 간 **관계**를 명시적으로 저장
- 패턴 기반 검색 (예: A와 B가 연결된 프로젝트 찾기)
- 복잡한 구조를 효율적으로 표현

### 1.3 YALDI의 Graph RAG

**목적:**
- 성공한 프로젝트의 **스키마 구조**를 그래프로 저장
- ERD 자동 생성 시 **유사 패턴을 검색**하여 참고

**예시:**
```
사용자: "예약 시스템 ERD 만들어줘"
↓
[Graph RAG] "예약" 관련 키워드로 Neo4j 검색
→ 결과: 호텔 예약 앱, 병원 예약 시스템, 식당 예약 플랫폼 (3개)
↓
[AI] 이 3개 프로젝트의 테이블 구조를 참고하여 ERD 생성
→ Booking, User, Payment, Reservation 테이블 포함
```

**왜 그래프인가?**
- 스키마는 **테이블 간 관계**가 핵심
- `User` → `Order` → `Product` 같은 관계를 그래프로 표현하면 직관적
- **패턴 매칭**으로 비슷한 구조를 빠르게 찾을 수 있음

---

## 2. 왜 Graph RAG를 사용하나?

### 2.1 문제: AI만으로는 부족하다

**Zero-Base 모드 (참고 없이 생성):**
```
질문: "도서 대여 서비스 ERD 만들어줘"
AI: Book, User, Rental 테이블 생성
→ 하지만 실제로는? Late Fee, Reservation, Review 등도 필요할 수 있음
→ AI 혼자서는 **모든 경우를 고려하기 어려움**
```

**Reference 모드 (유사 프로젝트 참고):**
```
질문: "도서 대여 서비스 ERD 만들어줘"
↓
[Graph RAG] 검색: "도서", "대여" 키워드
→ 결과: 도서관 시스템, 만화책 대여 앱, 전자책 플랫폼
↓
AI: 이 3개 프로젝트를 참고하여 설계
→ Book, User, Rental, Reservation (예약), Fine (연체료), Review (리뷰) 포함
→ **실무에서 검증된 구조**를 참고하여 더 완성도 높은 ERD 생성 ✅
```

### 2.2 Vector Search vs Graph RAG

**Vector Search (시맨틱 검색):**
- 의미적 유사도로 검색
- "예약 시스템"과 "booking platform"을 같다고 인식
- **장점**: 자연어 이해, 다국어 지원
- **단점**: **관계를 고려하지 않음**

**Graph RAG:**
- 엔티티 간 **관계 패턴**으로 검색
- "User → Booking → Room" 구조를 가진 프로젝트 찾기
- **장점**: **복잡한 관계 패턴 검색**
- **단점**: 키워드 기반, 의미는 이해 못함

**→ YALDI는 두 가지를 함께 사용**
1. **Graph RAG**: 유사한 **구조 패턴** 찾기
2. **Vector Search**: 의미적으로 **비슷한 프로젝트** 찾기 (시맨틱 검색 참고)

### 2.3 실제 효과

| 항목 | Zero-Base | Graph RAG 참고 |
|------|----------|---------------|
| 생성 시간 | 느림 | 빠름 (참고 자료 있음) |
| 완성도 | 낮음 | 높음 (검증된 패턴) |
| 놓치는 테이블 | 많음 | 적음 |
| 관계 정확도 | 보통 | 높음 |

---

## 3. Graph RAG 구조

### 3.1 Neo4j Graph 스키마

**노드 (Node):**
- `Project`: 프로젝트/버전 정보
- `Entity`: 테이블 (엔티티)

**관계 (Relationship):**
- `HAS_ENTITY`: 프로젝트가 테이블을 포함
- `RELATES_TO`: 테이블 간 관계 (1:N, N:M 등)

**그래프 구조:**
```
(Project: "호텔 예약 시스템")
    ↓ HAS_ENTITY
(Entity: "User")
    ↓ RELATES_TO (1:N)
(Entity: "Booking")
    ↓ RELATES_TO (N:1)
(Entity: "Room")
```

### 3.2 Neo4j Cypher 쿼리

**1. 프로젝트 저장 (인덱싱)**

```cypher
-- 프로젝트 노드 생성/업데이트
MERGE (p:Project {version_key: 123})
SET p.project_name = "호텔 예약 시스템",
    p.version_name = "v1.0",
    p.is_public = true,
    p.verification_status = "SUCCESS",
    p.indexed_at = datetime()

-- 엔티티 노드 생성
MATCH (p:Project {version_key: 123})
MERGE (e:Entity {name: "User", version_key: 123})
SET e.columns = ["id", "name", "email", "phone"]
MERGE (p)-[:HAS_ENTITY]->(e)

-- 관계 생성
MATCH (from:Entity {name: "User", version_key: 123})
MATCH (to:Entity {name: "Booking", version_key: 123})
MERGE (from)-[r:RELATES_TO {type: "ONE_TO_MANY"}]->(to)
```

**2. 유사 패턴 검색**

```cypher
-- 키워드로 프로젝트 검색
MATCH (p:Project)-[:HAS_ENTITY]->(e:Entity)
WHERE p.is_public = true
  AND p.verification_status = 'SUCCESS'
  AND any(keyword IN ["booking", "reservation"]
      WHERE toLower(e.name) CONTAINS toLower(keyword))
WITH p, count(DISTINCT e) as match_count
ORDER BY match_count DESC
LIMIT 3

-- 매칭된 프로젝트의 모든 엔티티와 관계 가져오기
MATCH (p)-[:HAS_ENTITY]->(entity:Entity)
OPTIONAL MATCH (entity)-[r:RELATES_TO]->(related:Entity)
RETURN p.version_key, p.project_name,
       collect({
           name: entity.name,
           columns: entity.columns,
           relations: collect({to: related.name, type: r.type})
       }) as entities
```

**검색 결과 예시:**
```json
[
  {
    "version_key": 45,
    "project_name": "호텔 예약 앱",
    "entities": [
      {
        "name": "User",
        "columns": ["id", "name", "email"],
        "relations": [{"to": "Booking", "type": "ONE_TO_MANY"}]
      },
      {
        "name": "Booking",
        "columns": ["id", "user_id", "room_id", "check_in"],
        "relations": [{"to": "Room", "type": "MANY_TO_ONE"}]
      }
    ]
  },
  ...
]
```

### 3.3 Python GraphRAG 클래스

**graph_rag.py 핵심 구조:**

```python
class GraphRAG:
    def __init__(self):
        # Neo4j 연결
        self.driver = AsyncGraphDatabase.driver(
            NEO4J_URI,
            auth=(NEO4J_USER, NEO4J_PASSWORD)
        )

    async def index_project_schema(
        self,
        version_key: int,
        project_name: str,
        schema_data: Dict,
        is_public: bool,
        design_verification_status: str
    ):
        """
        프로젝트를 Neo4j에 저장

        1. Project 노드 생성
        2. Entity 노드 생성 (테이블)
        3. RELATES_TO 관계 생성 (FK)
        """
        # Cypher 쿼리 실행
        ...

    async def search_similar_patterns(
        self,
        keywords: List[str],
        top_k: int = 5
    ) -> List[Dict]:
        """
        키워드로 유사 프로젝트 검색

        Returns:
            [
                {
                    "version_key": 45,
                    "project_name": "호텔 예약",
                    "entities": [...]
                }
            ]
        """
        # Cypher 쿼리 실행
        ...
```

---

## 4. 데이터 저장 흐름

### 4.1 전체 아키텍처

```
[User] 버전 저장 버튼 클릭
    ↓
[Backend] POST /api/v1/versions
    ↓ Version 엔티티 저장 (DB)
[Backend] Kafka 메시지 발행 → yaldi.version.verification
    ↓
[Kafka Consumer] VersionProcessingConsumerListener
    ↓
1. [AI Server] 스키마 검증 (Docker DB에서 BUILD)
    ↓ 성공/실패 판정
2. [AI Server] 임베딩 생성 (Vector 검색용)
    ↓
3. [AI Server] Neo4j Graph RAG 인덱싱 (검증 SUCCESS일 때만)
    ↓
[Neo4j] 그래프 저장 완료
```

### 4.2 상세 단계별 흐름

#### Step 1: 사용자 버전 저장
```
사용자가 ERD 작업 완료 후 "저장" 버튼 클릭
→ Backend가 Version 엔티티를 PostgreSQL에 저장
→ Kafka 메시지 발행
```

#### Step 2: Kafka Consumer 수신
```java
// VersionProcessingConsumerListener.java
@KafkaListener(topics = "yaldi.version.verification")
public void consumeVersionVerificationRequest(VersionProcessingMessage message) {
    // 1. 스키마 검증 (AI Server)
    VersionVerificationResult result = aiClient.verifySchema(
        message.schemaData(),
        message.versionName()
    );

    // 2. 검증 결과 저장
    version.updateVerificationStatus(result.status());

    // 3. Graph RAG 인덱싱 (검증 SUCCESS 시에만)
    if (verificationSuccess && status == SUCCESS) {
        graphRagAiClient.indexToGraph(
            versionKey, projectName, schemaData, isPublic, "SUCCESS"
        );
    }

    // 4. 임베딩 생성 (시맨틱 검색용)
    List<Double> vector = aiClient.generateEmbedding(...);
}
```

#### Step 3: AI Server - 스키마 검증
```python
# verification.py
@router.post("/version/verification")
async def verify_version(request: VersionVerificationRequest):
    # 1. SQL DDL 변환
    sql_ddl = convert_to_sql(request.schema_data)

    # 2. Docker DB에서 실제 BUILD
    result = execute_in_docker_db(sql_ddl)

    # 3. 성공/실패 판정
    if result.success:
        return VersionVerificationResponse(status="SUCCESS")
    else:
        # LLM으로 에러 분석 + 수정 제안
        return VersionVerificationResponse(
            status="FAILED",
            errors=[...],
            suggestions=[...]
        )
```

#### Step 4: AI Server - Graph RAG 인덱싱
```python
# graph_indexing.py
@router.post("/graph-rag/index")
async def index_to_graph(request: GraphIndexingRequest):
    graph_rag = await get_graph_rag()

    # Neo4j에 저장
    await graph_rag.index_project_schema(
        version_key=request.version_key,
        project_name=request.project_name,
        schema_data=request.schema_data,
        is_public=request.is_public,
        design_verification_status=request.design_verification_status
    )

    return {"success": True, "message": "인덱싱 완료"}
```

#### Step 5: Neo4j 저장
```python
# graph_rag.py - index_project_schema()
async def index_project_schema(self, version_key, schema_data, ...):
    async with self.driver.session() as session:
        # 1. Project 노드 생성
        await session.run("""
            MERGE (p:Project {version_key: $version_key})
            SET p.project_name = $project_name,
                p.is_public = $is_public,
                p.verification_status = $status
        """, ...)

        # 2. Entity 노드 생성
        for table in schema_data['tables']:
            await session.run("""
                MATCH (p:Project {version_key: $version_key})
                MERGE (e:Entity {name: $table_name, version_key: $version_key})
                SET e.columns = $columns
                MERGE (p)-[:HAS_ENTITY]->(e)
            """, ...)

        # 3. 관계 생성
        for relation in schema_data['relations']:
            await session.run("""
                MATCH (from:Entity {name: $from_table, version_key: $version_key})
                MATCH (to:Entity {name: $to_table, version_key: $version_key})
                MERGE (from)-[r:RELATES_TO {type: $rel_type}]->(to)
            """, ...)
```

### 4.3 언제 저장되나?

**조건:**
1. ✅ 사용자가 버전을 저장했을 때
2. ✅ 스키마 검증이 **SUCCESS**일 때만
3. ✅ `is_public = true` (공개 프로젝트만)

**저장 시점:**
- ❌ ERD 생성 요청 시점 (X)
- ✅ **버전 저장 → Kafka → 검증 SUCCESS → Graph 인덱싱**

---

## 5. 검색 흐름

### 5.1 ERD 생성 시 Graph RAG 활용

```
[User] "호텔 예약 시스템 ERD 만들어줘"
    ↓
[Backend] POST /api/v1/erd/generation
    ↓
[AI Server] ERD Workflow 시작
    ↓
[Domain Analyst Agent] 요구사항 분석
    → keywords: ["hotel", "booking", "reservation", "room"]
    ↓
[ERD Workflow] Graph RAG 검색 여부 판단
    → keywords가 있으면 REFERENCE 모드
    ↓
[GraphRAG.search_similar_patterns()] Neo4j 검색
    ↓ Cypher 쿼리 실행
[Neo4j] 유사 프로젝트 Top-3 반환
    ↓
[Schema Architect Agent] 참고하여 ERD 생성
    → REFERENCE 모드로 기존 패턴 활용
    ↓
[Validator Agent] 검증 → [Optimizer Agent] 최적화
    ↓
[User] 완성된 ERD 받음
```

### 5.2 검색 로직 상세

**erd_workflow.py - search_similar_node:**

```python
async def search_similar_node(self, state: ERDState) -> ERDState:
    """
    Graph RAG에서 유사 프로젝트 검색
    """
    keywords = state["keywords"]  # Domain Analyst가 추출한 키워드

    # Neo4j 검색
    graph_rag = await get_graph_rag()
    similar_projects = await graph_rag.search_similar_patterns(
        keywords=keywords,
        top_k=3  # 상위 3개만
    )

    if len(similar_projects) >= 1:
        state["mode"] = "REFERENCE"  # 참고 모드
        state["reference_data"] = similar_projects
    else:
        state["mode"] = "ZERO_BASE"  # 참고 없이 생성

    return state
```

**graph_rag.py - search_similar_patterns:**

```python
async def search_similar_patterns(self, keywords: List[str], top_k: int = 5):
    """
    키워드로 유사 프로젝트 검색

    1. 엔티티명에 키워드가 포함된 프로젝트 찾기
    2. 매칭된 엔티티 수로 정렬
    3. Top-K 프로젝트 반환
    """
    result = await session.run("""
        MATCH (p:Project)-[:HAS_ENTITY]->(e:Entity)
        WHERE p.is_public = true
          AND p.verification_status = 'SUCCESS'
          AND any(keyword IN $keywords
              WHERE toLower(e.name) CONTAINS toLower(keyword))
        WITH p, count(DISTINCT e) as match_count
        ORDER BY match_count DESC
        LIMIT $top_k

        MATCH (p)-[:HAS_ENTITY]->(entity:Entity)
        OPTIONAL MATCH (entity)-[r:RELATES_TO]->(related:Entity)
        RETURN p.version_key, p.project_name,
               collect({...}) as entities
    """, keywords=keywords, top_k=top_k)

    return projects
```

### 5.3 유사도 판단 기준

**질문: "유사도 33%는 어떻게 계산되나?"**

**답변:**
- Neo4j는 **매칭 개수**로만 정렬
- "유사도 33%"는 **AI Workflow에서 계산**

**예시:**
```
검색 키워드: ["user", "booking", "payment"]  (3개)

ProjectA:
- 매칭된 엔티티: User, Booking, Payment (3개)
- 유사도: 3/3 = 100%

ProjectB:
- 매칭된 엔티티: User, Booking (2개)
- 유사도: 2/3 = 67%

ProjectC:
- 매칭된 엔티티: User (1개)
- 유사도: 1/3 = 33%
```

**erd_workflow.py에서 유사도 필터링:**

```python
# 33% 이상만 REFERENCE 모드
if len(similar_projects) >= 1:
    # 유사도 계산
    for project in similar_projects:
        matched_count = count_matched_entities(project, keywords)
        similarity = matched_count / len(keywords)

        if similarity >= 0.33:  # 33% 이상
            state["mode"] = "REFERENCE"
```

---

## 6. 설계 결정

### 6.1 왜 Neo4j인가?

**다른 선택지:**
- PostgreSQL (관계형 DB)
- MongoDB (Document DB)
- Elasticsearch (검색 엔진)

**Neo4j를 선택한 이유:**

| 요구사항 | Neo4j | PostgreSQL | MongoDB |
|---------|-------|------------|---------|
| 관계 패턴 검색 | ✅ 최적화됨 | ❌ 복잡한 JOIN | ❌ 어려움 |
| 그래프 순회 | ✅ Cypher 쿼리 | ❌ 재귀 쿼리 필요 | ❌ 불가능 |
| 패턴 매칭 | ✅ 직관적 | ❌ 복잡함 | ❌ 어려움 |
| 성능 (관계 검색) | ✅ 빠름 | ❌ 느림 | ❌ 느림 |

**스키마는 그래프 구조:**
- 테이블 = 노드
- 관계 (FK) = 엣지
- **그래프 DB가 가장 자연스러움**

### 6.2 검증 SUCCESS만 저장하는 이유

**왜 실패한 프로젝트는 저장 안 하나?**

- ❌ 실패한 스키마는 **잘못된 패턴**
- ❌ AI가 참고하면 **같은 오류 반복**
- ✅ 성공한 스키마만 **검증된 지식**

**예시:**
```
실패한 스키마:
- User 테이블에 PK 없음 → BUILD 실패

이걸 Graph에 저장하면?
→ 다음 생성 시 AI가 이걸 참고
→ 또 PK 없는 테이블 생성 (잘못된 패턴 학습)
```

### 6.3 Public만 저장하는 이유

**is_public = true인 프로젝트만 인덱싱**

- 개인 프로젝트는 **타인이 참고하면 안 됨**
- 공개 프로젝트만 **공유 지식**으로 활용
- 개인정보, 회사 기밀 보호

### 6.4 Top-K = 3인 이유

**왜 3개만?**

- **너무 많으면**: AI가 혼란 (프롬프트 길어짐, 토큰 낭비)
- **너무 적으면**: 다양성 부족
- **3개가 적절**: 다양한 관점 + 토큰 효율

**실험 결과:**
- Top-1: 편향 심함
- Top-3: 균형적
- Top-5: 프롬프트 길어져서 비효율

### 6.5 Kafka 비동기 처리 이유

**왜 동기 처리 안 하고 Kafka를 쓰나?**

**동기 처리 시 문제:**
```
[User] 버전 저장 버튼 클릭
    ↓ (대기 시작)
[Backend] 스키마 검증 (30초)
    ↓
[Backend] 임베딩 생성 (10초)
    ↓
[Backend] Graph 인덱싱 (5초)
    ↓ (45초 후)
[User] 응답 받음 (너무 느림!!!)
```

**Kafka 비동기 처리:**
```
[User] 버전 저장 버튼 클릭
    ↓
[Backend] DB 저장 (0.1초)
    ↓ 즉시 응답
[User] "저장 완료!" (빠름!)
    ↓ (백그라운드에서 진행)
[Kafka Consumer] 검증, 임베딩, Graph 인덱싱 (45초)
    ↓
[Frontend SSE] 실시간 진행 상황 알림
```

**효과:**
- 사용자 대기 시간: 45초 → **0.1초**
- UX 향상
- 서버 부하 분산

### 6.6 실패 시 Graceful Degradation

**Graph RAG 인덱싱 실패해도 원본 데이터는 정상 처리**

```java
// VersionProcessingConsumerListener.java
try {
    graphRagAiClient.indexToGraph(...);
} catch (Exception e) {
    log.error("Graph RAG 인덱싱 실패 - 원본 데이터는 정상 처리됨", e);
    // 예외 던지지 않음, 계속 진행
}
```

**왜?**
- Graph RAG는 **보조 기능**
- 인덱싱 실패해도 버전 저장은 성공해야 함
- Neo4j 장애 시에도 서비스 동작

---

## 7. 실제 사례

### 예시 1: "예약 시스템" ERD 생성

**1. 사용자 요청:**
```
"호텔 예약 시스템 ERD 만들어줘"
```

**2. Domain Analyst 키워드 추출:**
```
keywords: ["hotel", "booking", "reservation", "room", "user", "payment"]
```

**3. Graph RAG 검색:**
```cypher
MATCH (p:Project)-[:HAS_ENTITY]->(e:Entity)
WHERE any(keyword IN ["hotel", "booking", "reservation", "room", "user", "payment"]
      WHERE toLower(e.name) CONTAINS toLower(keyword))
WITH p, count(DISTINCT e) as match_count
ORDER BY match_count DESC
LIMIT 3
```

**4. 검색 결과:**
```
ProjectA: "호텔 예약 앱" (5개 매칭: User, Booking, Room, Payment, Reservation)
ProjectB: "민박 예약 플랫폼" (4개 매칭: User, Booking, Room, Payment)
ProjectC: "식당 예약 시스템" (2개 매칭: User, Reservation)
```

**5. AI가 참고하여 생성:**
```
참고:
- ProjectA: User → Booking → Room, Payment
- ProjectB: User, Booking, Room
- ProjectC: User, Reservation

최종 ERD:
- User (id, name, email, phone)
- Hotel (id, name, address, rating)
- Room (id, hotel_id, room_number, type, price)
- Booking (id, user_id, room_id, check_in, check_out, status)
- Payment (id, booking_id, amount, method, paid_at)

관계:
- User → Booking (1:N)
- Booking → Room (N:1)
- Booking → Payment (1:1)
- Hotel → Room (1:N)
```

### 예시 2: "커뮤니티 게시판" ERD 생성

**1. 사용자 요청:**
```
"커뮤니티 게시판 ERD 만들어줘"
```

**2. 키워드:**
```
keywords: ["community", "post", "comment", "user", "board"]
```

**3. Graph RAG 검색 결과:**
```
ProjectX: "커뮤니티 플랫폼" (User, Post, Comment, Like)
ProjectY: "블로그 시스템" (User, Post, Comment, Tag)
ProjectZ: "Q&A 게시판" (User, Question, Answer, Vote)
```

**4. AI가 종합:**
```
공통 패턴:
- User
- Post (게시글)
- Comment (댓글)
- 좋아요/투표 기능

추가 고려:
- Tag (태그)
- Category (카테고리)

최종 ERD:
- User, Post, Comment, Like, Tag, Category
```

---

## 8. 결론

### 8.1 Graph RAG의 역할

1. **지식 축적**: 성공한 프로젝트를 그래프로 저장
2. **패턴 검색**: 유사한 구조를 빠르게 찾기
3. **AI 보조**: ERD 생성 시 참고 자료 제공

### 8.2 핵심 설계 원칙

1. **검증된 지식만 저장**: SUCCESS 프로젝트만 인덱싱
2. **공개 지식 공유**: Public 프로젝트만 활용
3. **비동기 처리**: Kafka로 사용자 경험 개선
4. **Graceful Degradation**: 실패해도 원본 데이터는 보호

### 8.3 기대 효과

- ERD 생성 **완성도 향상** (검증된 패턴 참고)
- 생성 **속도 향상** (Zero-Base보다 빠름)
- **실무 패턴 학습** (실제 프로젝트 기반)
- **집단 지성** (모든 사용자의 프로젝트가 지식으로 축적)

### 8.4 기술 스택 요약

| 컴포넌트 | 기술 | 역할 |
|---------|------|------|
| Graph DB | Neo4j | 관계 그래프 저장 |
| 쿼리 언어 | Cypher | 패턴 매칭 검색 |
| 메시징 | Kafka | 비동기 처리 |
| AI Framework | LangGraph | ERD Workflow |
| Backend | Spring Boot | Graph RAG Client |
