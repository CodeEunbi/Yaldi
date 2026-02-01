# ERD 상담 챗봇 AI 기술 설계 문서

## 1. 개요

### 기능 설명
사용자가 ERD 설계 중 궁금한 점을 질문하면 **10개 전문 분야 Expert Agent가 협업**하여 답변하는 AI 챗봇입니다.

### 핵심 가치
- **10개 전문가의 집단 지성** 활용
- **Intent Router가 필요한 전문가만 자동 선택** → 비용 70% 절감
- **병렬 실행**으로 3배 빠른 응답 (5초 내)
- **의견 충돌 시 Trade-off 분석** 제공
- **Self-Reflection**으로 답변 검증
- **대화 맥락 파악** (최대 20턴 히스토리 활용)

---

## 2. LangChain & LangGraph 아키텍처

### 2.1 LangChain의 역할

**LangChain = LLM 호출 및 프롬프트 관리 라이브러리**

```python
# base_expert.py:32-37
self.llm = ChatOpenAI( 
    base_url=settings.GMS_BASE_URL,
    api_key=settings.GMS_API_KEY,
    model="claude-sonnet-4-20250514",
    temperature=0.3
)
```

**LangChain이 제공하는 것**:
1. **통일된 LLM 인터페이스**: OpenAI, Anthropic, Google 등을 같은 방식으로 호출
2. **프롬프트 템플릿**: System/Human 메시지 구조화
3. **메모리 관리**: 대화 히스토리 자동 관리 (필요 시)
4. **출력 파싱**: LLM 응답을 JSON으로 자동 변환

**우리 프로젝트에서 사용하는 기능**:
```python
# LangChain의 메시지 구조
from langchain_core.messages import SystemMessage, HumanMessage

messages = [
    SystemMessage(content=system_prompt),  # 역할 정의
    HumanMessage(content=user_prompt)      # 실제 질문
]

response = await self.llm.ainvoke(messages)  # 비동기 호출
```

---

### 2.2 LangGraph의 역할

**LangGraph = Agent 워크플로우 오케스트레이션 라이브러리**

**왜 LangGraph를 쓰나?**
- 여러 Agent를 순서대로 실행해야 함
- 조건부 분기 필요 (Intent Router 결과에 따라 Expert 선택)
- 각 단계의 상태(State) 관리 필요

#### LangGraph의 핵심 개념

##### 1. State (상태)
```python
# consultation_workflow.py:32-44
class ConsultationState(TypedDict):
    # 입력
    message: str                    # 사용자 질문
    schema_data: Dict               # 현재 스키마
    conversation_history: List[Dict] # 대화 히스토리

    # 중간 결과
    intent_result: Dict             # Intent Router 결과
    agent_responses: Dict[str, Dict] # Expert 답변들

    # 최종 결과
    final_response: Dict            # 통합된 최종 답변
```

**State는 워크플로우 전체에서 공유**:
- Context Enrichment가 `message` 수정
- Intent Router가 `intent_result` 생성
- Expert Agents가 `agent_responses` 추가
- Aggregator가 `final_response` 생성

##### 2. Node (노드 = 각 단계)
```python
# consultation_workflow.py:203-206
workflow.add_node("context_enrichment", self.context_enrichment_node)
workflow.add_node("intent_routing", self.intent_routing_node)
workflow.add_node("expert_consultation", self.expert_consultation_node)
workflow.add_node("aggregation", self.aggregation_node)
```

**각 Node는 함수**:
```python
async def context_enrichment_node(self, state: ConsultationState) -> ConsultationState:
    # 1. State에서 데이터 읽기
    original_message = state["message"]

    # 2. 작업 수행
    enriched_message = await self.context_enricher.enrich(...)

    # 3. State 업데이트해서 반환
    state["message"] = enriched_message
    return state
```

##### 3. Edge (엣지 = 실행 흐름)
```python
# consultation_workflow.py:209-213
workflow.set_entry_point("context_enrichment")  # 시작점
workflow.add_edge("context_enrichment", "intent_routing")  # 순차 연결
workflow.add_edge("intent_routing", "expert_consultation")
workflow.add_edge("expert_consultation", "aggregation")
workflow.add_edge("aggregation", END)  # 종료
```

**실행 흐름 시각화**:
```
START
  ↓
[context_enrichment]
  ↓
[intent_routing]
  ↓
[expert_consultation]  ← 여기서 병렬 실행
  ↓
[aggregation]
  ↓
END
```

##### 4. 조건부 분기 (다른 워크플로우에서 사용 가능)
```python
# ERD 자동 생성에서 사용하는 예시
workflow.add_conditional_edges(
    "search_similar",
    decide_mode,  # 함수가 "reference" or "zero_base" 반환
    {
        "reference": "design_with_reference",
        "zero_base": "design_from_scratch"
    }
)
```

**상담 챗봇은 조건부 분기 없음** (순차 실행만)

---

### 2.3 LangChain vs LangGraph 차이

| | LangChain | LangGraph |
|---|-----------|-----------|
| **역할** | LLM 호출, 프롬프트 관리 | Agent 워크플로우 관리 |
| **단위** | 개별 LLM 호출 | 여러 Agent 조합 |
| **예시** | Expert Agent 하나 실행 | Context → Intent → Expert → Aggregator 전체 흐름 |
| **사용 레벨** | 각 Agent 내부 | 전체 시스템 오케스트레이션 |

**비유**:
- LangChain = 악기 하나 연주
- LangGraph = 오케스트라 지휘

---

## 3. Multi-Agent 아키텍처 설계

### 3.1 설계 의도 (Why Multi-Expert System?)

#### 단일 LLM의 한계
- 모든 ERD 분야를 완벽하게 답변하기 어려움
- 정규화, 인덱스, 확장성을 동시에 고려한 답변 불가능
- 일반적인 답변만 제공 (깊이 부족)

#### Multi-Expert 접근의 장점
1. **전문화 (Specialization)**: 각 Agent가 특정 분야만 깊이 있게 답변
2. **다각도 분석**: 한 질문을 여러 관점에서 분석
3. **Trade-off 제시**: 상충되는 의견도 솔직하게 제시
4. **확장성**: 새로운 전문 분야 쉽게 추가

---

### 3.2 Agent 구성 및 역할

전체 **12개 Agent**로 구성:

#### Agent 1: Context Enrichment Agent (맥락 보강)
**역할**: 짧거나 애매한 질문을 대화 히스토리를 참고해서 명확하게 재구성

**입력**:
- 사용자 질문: "어 해줘"
- 대화 히스토리: 최근 **10턴**

**처리 로직**:
```python
# context_enrichment.py:81-96
def _needs_enrichment(self, question, history):
    # 1. 10자 미만 → 재구성 필요
    if len(question) < 10:
        return True

    # 2. "어", "응", "그거", "해줘" 포함 → 재구성 필요
    if any(pattern in question for pattern in SHORT_PATTERNS):
        return True

    return False
```

**출력**:
- 재구성된 질문: "User 테이블을 3NF로 정규화해주세요"

**설계 고려사항**:
- Temperature 0.0 (원래 의도를 정확히 파악)
- 최근 10턴만 참고 (토큰 절약)
- 재구성 실패 시 원본 그대로 반환 (안전)

**사용 모델**: Claude Sonnet 4 (`claude-sonnet-4-20250514`)

---

#### Agent 2: Intent Router Agent (질문 분류기)
**역할**: 질문을 10개 카테고리로 분류하여 필요한 Expert만 선택

**10개 카테고리**:
1. **Normalization** (정규화): 1NF, 2NF, 3NF, BCNF, 역정규화
2. **PKSelection** (PK 선택): AUTO_INCREMENT vs UUID vs Natural Key
3. **Relationship** (관계 설정): 1:1, 1:N, N:M, FK 배치
4. **DataType** (데이터 타입): VARCHAR vs TEXT, INT vs BIGINT
5. **Constraint** (제약 조건): NOT NULL, UNIQUE, CHECK, DEFAULT
6. **Directionality** (방향성): 단방향 vs 양방향, 순환 참조
7. **ManyToMany** (N:M 관계): 중간 테이블 설계, 추가 속성
8. **IndexStrategy** (인덱스 전략): 단일/복합 인덱스, 커버링 인덱스
9. **Scalability** (확장성): 샤딩, 파티셔닝, Read Replica
10. **BestPractice** (베스트 프랙티스): 네이밍 컨벤션, 소프트 삭제

**Multi-Label Classification**:
```json
{
  "confidence": {
    "Normalization": 0.92,    // ✅ 선택됨
    "IndexStrategy": 0.85,    // ✅ 선택됨
    "Scalability": 0.55,      // ❌ threshold 미만
    "PKSelection": 0.20       // ❌ threshold 미만
  },
  "categories": ["Normalization", "IndexStrategy"],
  "is_general": false
}
```

**Confidence Threshold = 0.6**:
- 0.6 이상만 Expert 실행
- 너무 낮으면 관련 없는 답변 방지
- 너무 높으면 필요한 Expert 누락

**설계 고려사항**:
- Temperature 0.0 (완전 결정론적 분류)
- 대화 히스토리 최근 **3턴** 참고 (맥락 파악)
- Fallback: 분류 실패 시 `is_general: true` → GeneralAdviceAgent 실행

**사용 모델**: Claude Sonnet 4 (`claude-sonnet-4-20250514`)

---

#### Agent 3~12: 10개 Expert Agent + General Advice Agent

모든 Expert Agent는 **BaseExpertAgent 클래스 상속**:

| Agent | 전문 분야 | 프롬프트 파일 |
|-------|----------|--------------|
| **NormalizationExpert** | 정규화 (1NF~BCNF, 역정규화) | `normalization_system.txt` |
| **PKSelectionExpert** | PK 선택 (AUTO_INCREMENT, UUID, Natural Key) | `pk_selection_system.txt` |
| **RelationshipExpert** | 관계 설정 (1:1, 1:N, N:M, Cascade) | `relationship_system.txt` |
| **DataTypeExpert** | 데이터 타입 (VARCHAR vs TEXT 등) | `data_type_system.txt` |
| **ConstraintExpert** | 제약 조건 (NOT NULL, UNIQUE 등) | `constraint_system.txt` |
| **DirectionalityExpert** | 방향성 (단방향, 양방향, 순환 참조) | `directionality_system.txt` |
| **ManyToManyExpert** | N:M 관계 (중간 테이블 설계) | `many_to_many_system.txt` |
| **IndexStrategyExpert** | 인덱스 전략 (복합 인덱스 등) | `index_strategy_system.txt` |
| **ScalabilityExpert** | 확장성 (샤딩, 파티셔닝) | `scalability_system.txt` |
| **BestPracticeExpert** | 베스트 프랙티스 (네이밍 등) | `best_practice_system.txt` |
| **GeneralAdviceAgent** | 일반 조언 (Fallback용) | `general_system.txt` |

#### Expert Agent 공통 구조 (BaseExpertAgent)

**입력**:
- 사용자 질문
- 현재 스키마 데이터 (최대 10개 테이블로 요약)
- 대화 히스토리 (최근 **3턴**)

**출력**:
```json
{
  "answer": "정규화는 데이터 중복을 제거하여 무결성을 보장하는 기법입니다...",
  "confidence": 0.9,
  "schema_modifications": [
    {
      "action": "SPLIT_TABLE",
      "description": "users 테이블을 users와 user_profiles로 분리",
      "details": {
        "from_table": "users",
        "new_tables": ["users", "user_profiles"],
        "reason": "프로필 정보와 기본 정보 분리로 3NF 달성"
      }
    }
  ],
  "warnings": ["기존 FK 관계 수정 필요"],
  "references": ["3NF 원칙", "함수 종속성"]
}
```

**Self-Reflection (답변 검증)**:
```python
# base_expert.py:137-200
async def _self_reflect(self, result, schema_data):
    # 검증 항목:
    # 1. 스키마 일관성: 제안한 테이블이 실제로 존재하는지
    # 2. 논리적 모순: ADD_INDEX인데 테이블이 없으면 경고
    # 3. 검증 실패 시 confidence 0.1 차감

    existing_tables = {"users", "posts", "comments"}

    for modification in result["schema_modifications"]:
        if modification["action"] == "ADD_INDEX":
            table = modification["details"]["table"]
            if table not in existing_tables:
                warnings.append(f"{table} 테이블이 현재 스키마에 없습니다")
                confidence -= 0.1
```

**설계 고려사항**:
- Temperature 0.3 (창의성과 일관성 균형)
- 스키마 요약으로 토큰 절약 (최대 10개 테이블만 전달)
- Self-Reflection으로 환각 방지
- 대화 히스토리 최근 **3턴**만 (토큰 절약)

**사용 모델**: Claude Sonnet 4 (`claude-sonnet-4-20250514`)

---

#### Agent 13: Response Aggregator Agent (답변 통합기)
**역할**: 여러 Expert 답변을 하나로 통합

**통합 전략**:

##### Case 1: 의견 일치
```markdown
[정규화 관점]
User 테이블을 users와 user_profiles로 분리하세요.

[인덱스 전략 관점]
분리 후 user_id에 인덱스를 추가하세요.

[종합 권장사항]
두 전문가 모두 테이블 분리를 권장합니다.
우선순위: 1) 정규화 → 2) 인덱스 추가
```

##### Case 2: 의견 상충 → Trade-off 분석
```markdown
[의견 A - 정규화 전문가]
테이블을 분리하세요. (데이터 무결성 향상)

[의견 B - 확장성 전문가]
JOIN 비용을 고려하면 통합 유지가 나을 수 있습니다.

[Trade-off 분석]
✅ 데이터 무결성 우선 → 의견 A (분리)
   - 장점: 중복 제거, 업데이트 이상 방지
   - 단점: JOIN 비용 증가

✅ 조회 성능 우선 → 의견 B (통합)
   - 장점: 단일 쿼리로 조회 가능
   - 단점: 데이터 중복 가능성

✅ 균형 잡힌 접근 → 부분 정규화
   - 자주 조회하는 컬럼은 통합 유지
   - 거의 안 쓰는 컬럼만 분리

[권장사항]
- 쓰기 작업이 많고 무결성이 중요 → 분리
- 읽기 작업이 대부분이고 성능 중요 → 통합
- 현재 프로젝트 특성: {사용자 입력} → {맞춤 추천}
```

**Confidence 계산**:
- 의견 일치: 평균 확신도
- 의견 상충: 평균 - 0.1 (불확실성 반영)

**낮은 확신도 처리** (< 0.5):
```python
# aggregator.py:211-269
async def _suggest_question_refinement(self, user_question, current_result):
    # LLM에게 질문 재구성 제안 요청
    return {
        "refined_suggestions": [
            "User 테이블 정규화 방법이 궁금하신가요?",
            "PK 선택 기준을 알고 싶으신가요?",
            "인덱스 추가 위치를 추천받고 싶으신가요?"
        ],
        "reason": "질문이 너무 광범위하여 구체적인 답변이 어렵습니다."
    }
```

**설계 고려사항**:
- Temperature 0.2 (안정적인 통합)
- schema_modifications 중복 제거 (같은 action + table + column)
- 낮은 확신도 시 질문 재구성 제안으로 사용자 유도

**사용 모델**: Claude Sonnet 4 (`claude-sonnet-4-20250514`)

---

### 3.3 Agent 실행 흐름 (LangGraph Workflow)

#### 전체 플로우
```
[유저 질문]
   ↓
━━━━━━━━━━━ LangGraph 워크플로우 시작 ━━━━━━━━━━━

1. Context Enrichment
   - "어 해줘" → "User 테이블 정규화 진행"
   - 대화 히스토리 최근 10턴 참고
   ↓

2. Intent Router
   - 질문 분류 (Multi-Label)
   - confidence >= 0.6인 Expert 선택
   - 대화 히스토리 최근 3턴 참고
   ↓

3. Expert Consultation [병렬 실행] ⚡
   ┌─ Normalization Expert
   ├─ IndexStrategy Expert
   └─ Scalability Expert
   (asyncio.gather로 동시 실행)
   ↓

4. Response Aggregator
   - 답변 통합
   - Trade-off 분석 (의견 상충 시)
   - 최종 confidence 계산
   ↓

━━━━━━━━━━━ LangGraph 워크플로우 종료 ━━━━━━━━━━━

[사용자에게 반환]
```

#### 병렬 실행 코드 분석

```python
# consultation_workflow.py:159-178
# 선택된 Expert들의 Task 생성
tasks = []
for category in ["Normalization", "IndexStrategy"]:
    expert = self._get_expert(category)
    task = expert.consult(
        user_question=state["message"],
        schema_data=state["schema_data"],
        conversation_history=state["conversation_history"]
    )
    tasks.append((category, task))

# asyncio.gather로 병렬 실행
results = await asyncio.gather(
    *[task for _, task in tasks],
    return_exceptions=True  # 일부 실패해도 나머지 실행
)

# 결과 수집
for (category, _), result in zip(tasks, results):
    if isinstance(result, Exception):
        logger.error(f"{category} failed")
        continue
    responses[f"{category}Expert"] = result
```

**병렬 실행 전략**:
- Expert Agent들은 **서로 독립적** (순차 의존성 없음)
- 동시 실행으로 **3배 속도 향상** (15초 → 5초)
- 일부 실패해도 나머지 결과 활용

**왜 병렬 실행이 가능한가?**
```
질문: "User 테이블 정규화하고 인덱스도 추가하고 싶어요"

[Normalization Expert]
- 입력: 질문 + 현재 스키마
- 출력: 정규화 조언
- 다른 Expert 결과 불필요 ✅

[IndexStrategy Expert]
- 입력: 질문 + 현재 스키마
- 출력: 인덱스 조언
- 다른 Expert 결과 불필요 ✅

→ 두 Expert는 서로 독립적으로 실행 가능!
```

---

## 4. 대화 맥락 파악 (Conversation History)

### 4.1 히스토리 관리 구조

```python
# consultation.py:27-30
conversation_history: List[ConversationMessage] = Field(
    default_factory=list,
    max_items=20,  # 최대 20턴
    description="최근 대화 히스토리"
)

class ConversationMessage(BaseModel):
    role: str  # "user" or "assistant"
    content: str
```

**프론트엔드에서 전송하는 형식**:
```json
{
  "message": "User 테이블 정규화해줘",
  "schema_data": {...},
  "conversation_history": [
    {"role": "user", "content": "PK를 뭘로 하면 좋을까요?"},
    {"role": "assistant", "content": "AUTO_INCREMENT를 추천합니다..."},
    {"role": "user", "content": "정규화도 해야 하나요?"},
    {"role": "assistant", "content": "네, 3NF까지 추천합니다..."},
    {"role": "user", "content": "User 테이블 정규화해줘"}
  ]
}
```

### 4.2 각 Agent의 히스토리 활용 범위

| Agent | 히스토리 범위 | 용도 |
|-------|-------------|------|
| **Context Enrichment** | 최근 **10턴** | 짧은 질문 재구성 ("어 해줘" → 구체적 질문) |
| **Intent Router** | 최근 **3턴** | 맥락 기반 질문 분류 |
| **Expert Agents** | 최근 **3턴** | 맥락 기반 전문 답변 |
| **Aggregator** | 사용 안 함 | Expert 답변만 통합 |

**왜 범위가 다른가?**

#### Context Enrichment: 10턴
```python
# context_enrichment.py:124
for msg in conversation_history[-10:]:  # 최근 10턴
```
- "어 해줘" 같은 짧은 표현을 해석하려면 **충분한 맥락** 필요
- 10턴 전에 "정규화"를 언급했을 수도 있음

#### Intent Router & Expert: 3턴
```python
# intent_router.py:85-88
if conversation_history:
    recent = conversation_history[-3:]  # 최근 3턴만
```
- 너무 긴 히스토리는 토큰 낭비
- 직전 대화만 참고해도 충분 (현재 질문과 직접 연관)

### 4.3 맥락 파악 예시

**대화 시나리오**:
```
Turn 1:
User: "User 테이블에 어떤 컬럼이 필요할까요?"
AI: "id, email, name, created_at을 추천합니다."

Turn 2:
User: "PK는?"
AI: "id를 AUTO_INCREMENT로 설정하세요."

Turn 3:
User: "정규화도 해야 해?"
AI: "사용자 프로필 정보가 많으면 분리를 고려하세요."

Turn 4:
User: "어 해줘"  👈 애매한 질문
```

**Context Enrichment 처리**:
```python
# 최근 10턴 참고 (Turn 1~4 모두 봄)
히스토리 분석:
- Turn 3: "정규화도 해야 해?" 질문함
- Turn 3 답변: "분리를 고려하세요" 제안받음
- Turn 4: "어 해줘" = 정규화 실행 요청으로 해석

재구성된 질문:
"User 테이블을 정규화하여 users와 user_profiles로 분리해주세요"
```

**Intent Router 처리**:
```python
# 최근 3턴만 참고 (Turn 2~4)
맥락:
- Turn 3에서 "정규화" 언급
- Turn 4에서 재구성된 질문에 "정규화" 포함

분류 결과:
{
  "categories": ["Normalization"],
  "confidence": {"Normalization": 0.95}
}
```

---

## 5. 설계 철학 및 핵심 결정

### 5.1 왜 10개 전문 분야로 나눴나?

**Too Few (2~3개)**:
- 각 분야가 너무 광범위 → 깊이 부족
- 예: "DB 설계 전문가" → 모든 걸 다 알아야 함

**Too Many (20개 이상)**:
- 관리 복잡도 증가
- 프롬프트 유지보수 어려움
- 중복되는 분야 발생

**10개의 장점**:
- ERD 설계의 핵심 영역 모두 커버
- 각 분야가 명확하게 구분됨
- 관리 가능한 수준

---

### 5.2 Intent Router vs All Expert 실행

#### 왜 모든 Expert를 실행하지 않나?

**비용 문제**:
- 10개 Expert 모두 실행 → Claude API 비용 10배
- "PK 선택 질문"에 확장성 전문가가 답변할 필요 없음

**품질 문제**:
- 관련 없는 Expert가 억지로 답변 → 혼란
- 집중된 답변이 더 정확

**Intent Router 방식의 장점**:
1. **비용 절감**: 평균 2~3개 Expert만 실행 → **70% 절감**
2. **품질 향상**: 관련 있는 전문가만 답변
3. **응답 속도**: 불필요한 LLM 호출 제거

**실제 비용 비교**:
```
모든 Expert 실행:
- 10개 Expert × Claude API 호출 = 높은 비용

Intent Router 사용:
- Intent Router 1회 (경량 작업)
- 평균 2~3개 Expert만 실행
→ 70% 비용 절감
```

---

### 5.3 병렬 실행 설계

**병렬 실행이 가능한 이유**:
```
질문: "User 테이블 정규화하고 인덱스도 추가하고 싶어요"

[Normalization Expert]
- 입력: 질문 + 현재 스키마
- 독립 실행 가능 ✅

[IndexStrategy Expert]
- 입력: 질문 + 현재 스키마
- 독립 실행 가능 ✅

→ 두 Expert는 서로의 결과를 기다릴 필요 없음!
```

**성능 향상**:
```
순차 실행: Expert 1 (5초) → Expert 2 (5초) → Expert 3 (5초) = 15초
병렬 실행: Expert 1, 2, 3 동시 실행 = 5초

→ 3배 속도 향상
```

---

### 5.4 Self-Reflection 전략

**왜 필요한가?**
- LLM이 환각(Hallucination)으로 없는 테이블 언급 가능
- "users 테이블에 인덱스 추가" → users 테이블이 실제로 없을 수도

**검증 항목**:
```python
# base_expert.py:155-180
# 1. 테이블 존재 확인
existing_tables = {"users", "posts", "comments"}  # 현재 스키마

for modification in schema_modifications:
    if modification["action"] == "ADD_INDEX":
        table = modification["details"]["table"]
        if table not in existing_tables:
            # 경고 추가
            warnings.append(f"{table} 테이블이 현재 스키마에 없습니다")
            # Confidence 감소
            confidence -= 0.1
```

**효과**:
- 잘못된 제안 필터링
- 사용자에게 경고 제공
- 신뢰도 하락 표시 (Confidence 조정)

---

## 6. 모델 선택 전략

### 6.1 Claude Sonnet 4 단일 모델 사용

**모델**: `claude-sonnet-4-20250514`

**모든 Agent에서 동일 모델 사용**:

| Agent | 모델 | Temperature |
|-------|------|-------------|
| Context Enrichment | Claude Sonnet 4 | 0.0 |
| Intent Router | Claude Sonnet 4 | 0.0 |
| 10개 Expert Agents | Claude Sonnet 4 | 0.3 |
| Response Aggregator | Claude Sonnet 4 | 0.2 |

**단일 모델 선택 이유**:
1. **일관된 출력 포맷**: JSON 파싱 안정성
2. **긴 답변 생성 능력**: Expert Agent에서 중요
3. **구조화된 출력**: schema_modifications 등 복잡한 JSON
4. **세밀한 뉘앙스**: "경우에 따라 다름" 같은 조건부 답변

### 6.2 Temperature 설정 전략

| Agent | Temperature | 이유 |
|-------|-------------|------|
| Context Enrichment | 0.0 | 원래 의도를 정확히 파악 (결정론적) |
| Intent Router | 0.0 | 분류는 일관되게 (결정론적) |
| Expert Agents | 0.3 | 창의성과 일관성 균형 |
| Aggregator | 0.2 | 안정적인 통합 |

**Temperature 0.0 vs 0.3 차이**:
- 0.0: 항상 같은 입력 → 같은 출력 (분류, 재구성)
- 0.3: 약간의 변화 허용 (답변 다양성)

---

## 7. 프롬프트 엔지니어링 전략

### 7.1 프롬프트 파일 분리 관리

**디렉토리 구조**:
```
AI/yaldi/prompts/consultation/
├── intent_router_system.txt      # Intent Router 시스템 프롬프트
├── intent_router_user.txt         # Intent Router 유저 프롬프트
├── aggregator_system.txt          # Aggregator 시스템 프롬프트
├── aggregator_user.txt            # Aggregator 유저 프롬프트
└── experts/
    ├── normalization_system.txt   # 정규화 전문가
    ├── pk_selection_system.txt    # PK 선택 전문가
    ├── relationship_system.txt    # 관계 설정 전문가
    ├── data_type_system.txt       # 데이터 타입 전문가
    ├── constraint_system.txt      # 제약 조건 전문가
    ├── directionality_system.txt  # 방향성 전문가
    ├── many_to_many_system.txt    # N:M 관계 전문가
    ├── index_strategy_system.txt  # 인덱스 전략 전문가
    ├── scalability_system.txt     # 확장성 전문가
    ├── best_practice_system.txt   # 베스트 프랙티스 전문가
    ├── general_system.txt         # 일반 조언 (Fallback)
    └── common_user.txt            # 공통 유저 프롬프트
```

**분리 관리의 장점**:
1. **유지보수 용이**: 코드 수정 없이 프롬프트만 변경
2. **버전 관리**: Git으로 프롬프트 변경 이력 추적
3. **협업**: 비개발자도 프롬프트 수정 가능

---

### 7.2 Intent Router 프롬프트 설계

#### Multi-Label Classification 전략

**핵심**: 한 질문이 여러 카테고리에 해당 가능

```
질문: "User 테이블 정규화하면서 성능도 고려하고 싶어요"

출력:
{
  "confidence": {
    "Normalization": 0.92,     // 정규화 명시
    "IndexStrategy": 0.80,     // 성능 = 인덱스
    "Scalability": 0.50,       // 약간 관련
    "PKSelection": 0.10        // 거의 무관
  }
}
```

#### Few-Shot Learning

```
예시 1:
질문: "User 테이블을 정규화해야 하나요?"
→ Normalization: 0.95

예시 2:
질문: "PK를 뭘로 설정하면 좋을까요?"
→ PKSelection: 0.90

예시 3:
질문: "정규화하면서 성능도 고려하고 싶어요"
→ Normalization: 0.85, IndexStrategy: 0.75
```

---

### 7.3 Expert Agent 프롬프트 설계

#### 공통 구조

**System Prompt** (`{category}_system.txt`):
```
당신은 {분야} 전문가입니다.

전문 지식:
- {핵심 개념 1}
- {핵심 개념 2}
- {핵심 개념 3}

답변 시 포함사항:
1. 명확한 설명 (초보자도 이해 가능)
2. 실제 적용 예시
3. Trade-off 분석 (장단점)
4. 스키마 수정 제안 (JSON)

주의사항:
- "무조건 ~해야 한다" 피하기
- 상황별 권장사항 제시
- 불확실하면 확신도 낮추기

JSON 형식:
{
  "answer": "...",
  "confidence": 0.9,
  "schema_modifications": [...],
  "warnings": [...],
  "references": [...]
}
```

**User Prompt** (공통 `common_user.txt`):
```
사용자 질문:
{user_question}

현재 스키마:
{schema_summary}

최근 대화:
{context}

위 질문에 {분야} 전문가 입장에서 답변하세요.
```

---

### 7.4 Aggregator 프롬프트 설계

#### Trade-off 분석 전략

```
여러 전문가 의견을 통합하되, 상충되는 경우:

1. 각 관점 명확히 구분
[정규화 전문가] ...
[성능 전문가] ...

2. 장단점 비교
[Trade-off]
- 무결성 우선: 분리
- 성능 우선: 통합

3. 상황별 권장사항
[권장사항]
- 쓰기 작업 많음 → 분리
- 읽기 작업 많음 → 통합
```

---

## 8. 최적화 전략

### 8.1 비용 최적화

**1회 상담 시 LLM 호출**:
```
Context Enrichment: 1회 (재구성 필요 시만)
Intent Router: 1회
Expert Agents: 0~10회 (평균 2~3회)
Response Aggregator: 1회

총: 평균 4~5회
```

**비용 절감 방법**:
1. **Intent Router로 필요한 Expert만 실행** → 70% 절감
2. **스키마 요약** (최대 10개 테이블) → 토큰 절약
3. **대화 히스토리 제한** (Context: 10턴, 나머지: 3턴) → 토큰 절약

---

### 8.2 성능 최적화

**병렬 실행**:
```python
# asyncio.gather로 동시 실행
results = await asyncio.gather(
    normalization_expert.consult(...),
    index_expert.consult(...),
    scalability_expert.consult(...)
)
```
→ **3배 속도 향상** (15초 → 5초)

**스키마 요약**:
```python
# base_expert.py:117-135
def _summarize_schema(self, schema_data):
    # 최대 10개 테이블만
    summary = "테이블 수: 25\n"
    for table in tables[:10]:
        summary += f"- {table.name} ({len(columns)}개 컬럼)\n"
    summary += "... (외 15개 테이블)"
```

---

### 8.3 정확도 향상 전략

1. **Self-Reflection**: Expert 답변 검증 후 confidence 조정
2. **Confidence Threshold (0.6)**: 관련 없는 Expert 실행 방지
3. **낮은 확신도 시 질문 재구성 제안**: 사용자 의도 명확화
4. **Trade-off 분석**: 상충 의견 솔직하게 제시 → 신뢰도 향상

---

## 9. 핵심 설계 결정 요약

| 설계 항목 | 결정 | 이유 |
|-----------|------|------|
| **Agent 구조** | 12개 Agent (10개 Expert + 2개 보조) | 전문화, 확장성 |
| **실행 방식** | 병렬 실행 (Expert만) | 독립 실행 가능, 3배 빠름 |
| **Expert 선택** | Intent Router 자동 선택 | 비용 70% 절감 |
| **Threshold** | Confidence >= 0.6 | 관련성 보장 |
| **답변 통합** | Trade-off 분석 제공 | 상충 의견 솔직 제시 |
| **모델** | Claude Sonnet 4 단일 | 일관성, 긴 답변 생성 |
| **검증** | Self-Reflection | 환각 방지 |
| **대화 맥락** | 최대 20턴 (활용: 3~10턴) | 토큰 절약, 충분한 맥락 |
| **워크플로우** | LangGraph | 상태 관리, 순차/병렬 혼합 |
| **LLM 호출** | LangChain | 통일된 인터페이스 |

---

## 10. 발표 핵심 메시지

### 기술적 차별점
1. **Multi-Expert System** → 10개 전문 분야 깊이 있는 답변
2. **Intent Router** → 필요한 전문가만 선택 (비용 70% 절감)
3. **병렬 실행** → 3배 빠른 응답 (5초 내)
4. **Trade-off 분석** → 상충 의견도 솔직하게 제시
5. **Self-Reflection** → 환각 방지, 품질 보장
6. **LangGraph** → 복잡한 워크플로우 관리
7. **대화 맥락 파악** → 최대 20턴 히스토리 활용

### 사용자 가치
- **10개 전문가의 집단 지성** 활용
- **실시간 ERD 설계 조언** (5초 내)
- **구체적인 스키마 수정 제안** (JSON 형식으로 적용 가능)
- **상황별 Trade-off 제시** → 프로젝트 특성에 맞는 선택
- **대화형 상담** → "어 해줘" 같은 짧은 표현도 이해
