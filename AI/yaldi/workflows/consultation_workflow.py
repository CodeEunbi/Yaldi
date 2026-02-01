"""
LangGraph 기반 ERD 상담 챗봇 워크플로우

Context Enrichment → Intent Routing → Parallel Expert Execution → Response Aggregation
"""
from langgraph.graph import StateGraph, END
from typing import TypedDict, List, Dict
import asyncio
import logging
from core.singleton import SingletonMeta

from agents.consultation import (
    IntentRouterAgent,
    NormalizationExpert,
    PKSelectionExpert,
    RelationshipExpert,
    DataTypeExpert,
    ConstraintExpert,
    DirectionalityExpert,
    ManyToManyExpert,
    IndexStrategyExpert,
    ScalabilityExpert,
    BestPracticeExpert,
    GeneralAdviceAgent,
    ResponseAggregator,
    ContextEnrichmentAgent
)

logger = logging.getLogger(__name__)


# State 정의
class ConsultationState(TypedDict):
    # 입력
    project_key: int
    message: str
    schema_data: Dict
    conversation_history: List[Dict]

    # 중간 결과
    intent_result: Dict  # Intent Router 결과
    agent_responses: Dict[str, Dict]  # 각 Expert 답변

    # 최종 결과
    final_response: Dict


class ConsultationWorkflow(metaclass=SingletonMeta):
    """
    ERD 상담 챗봇 LangGraph 워크플로우

    플로우:
    0. Context Enrichment: 짧은 질문 재구성 (예: "어 해줘" → "User 테이블 정규화를 진행해주세요")
    1. Intent Router: 질문 분류
    2. Expert Agents: 병렬 실행 (선택된 Agent만)
    3. Response Aggregator: 답변 통합

    Singleton 패턴 적용: 워크플로우 그래프를 한 번만 빌드
    """

    # Expert Agent 매핑
    EXPERT_MAP = {
        "Normalization": NormalizationExpert,
        "PKSelection": PKSelectionExpert,
        "Relationship": RelationshipExpert,
        "DataType": DataTypeExpert,
        "Constraint": ConstraintExpert,
        "Directionality": DirectionalityExpert,
        "ManyToMany": ManyToManyExpert,
        "IndexStrategy": IndexStrategyExpert,
        "Scalability": ScalabilityExpert,
        "BestPractice": BestPracticeExpert
    }

    def __init__(self):
        # Singleton이므로 중복 초기화 방지
        if hasattr(self, '_initialized'):
            return
        self._initialized = True

        self.context_enricher = ContextEnrichmentAgent()
        self.intent_router = IntentRouterAgent()
        self.general_agent = GeneralAdviceAgent()
        self.aggregator = ResponseAggregator()

        # Expert Agents 인스턴스 (lazy initialization)
        self.experts = {}

        logger.info("Consultation Workflow singleton instance created")

    def _get_expert(self, category: str):
        """Expert Agent 인스턴스 가져오기 (싱글톤)"""
        if category not in self.experts:
            expert_class = self.EXPERT_MAP.get(category)
            if expert_class:
                self.experts[category] = expert_class()
            else:
                logger.warning(f"Unknown category: {category}, using GeneralAgent")
                return self.general_agent
        return self.experts[category]

    async def context_enrichment_node(self, state: ConsultationState) -> ConsultationState:
        """0. Context Enrichment: 짧은/애매한 질문 재구성"""
        logger.info("[Workflow] Step 0: Context Enrichment")
        logger.info(f"[Workflow] Original question: {state['message']}")

        enriched_message = await self.context_enricher.enrich(
            user_question=state["message"],
            conversation_history=state.get("conversation_history")
        )

        # 재구성된 질문으로 업데이트
        if enriched_message != state["message"]:
            logger.info(f"[Workflow] Enriched question: {enriched_message}")
            state["message"] = enriched_message
        else:
            logger.info("[Workflow] Question is clear, no enrichment needed")

        return state

    async def intent_routing_node(self, state: ConsultationState) -> ConsultationState:
        """1. Intent Router: 질문 분류"""
        logger.info("[Workflow] Step 1: Intent Routing")

        intent_result = await self.intent_router.route(
            user_question=state["message"],
            conversation_history=state.get("conversation_history")
        )

        state["intent_result"] = intent_result
        logger.info(f"[Workflow] Selected categories: {intent_result['categories']}")

        return state

    async def expert_consultation_node(self, state: ConsultationState) -> ConsultationState:
        """2. Expert Agents: 병렬 실행"""
        logger.info("[Workflow] Step 2: Expert Consultation")

        intent_result = state["intent_result"]

        # GeneralAdviceAgent만 실행하는 경우
        if intent_result.get("is_general", False):
            logger.info("[Workflow] Using GeneralAdviceAgent")
            response = await self.general_agent.consult(
                user_question=state["message"],
                schema_data=state["schema_data"],
                conversation_history=state.get("conversation_history")
            )
            state["agent_responses"] = {"GeneralAdviceAgent": response}
            return state

        # 선택된 Expert Agents 병렬 실행
        categories = intent_result.get("categories", [])

        if not categories:
            # Fallback
            logger.warning("[Workflow] No categories selected, using GeneralAgent")
            response = await self.general_agent.consult(
                user_question=state["message"],
                schema_data=state["schema_data"],
                conversation_history=state.get("conversation_history")
            )
            state["agent_responses"] = {"GeneralAdviceAgent": response}
            return state

        # 병렬 실행
        tasks = []
        for category in categories:
            expert = self._get_expert(category)
            task = expert.consult(
                user_question=state["message"],
                schema_data=state["schema_data"],
                conversation_history=state.get("conversation_history")
            )
            tasks.append((category, task))

        # 모든 Agent 실행
        responses = {}
        results = await asyncio.gather(*[task for _, task in tasks], return_exceptions=True)

        for (category, _), result in zip(tasks, results):
            if isinstance(result, Exception):
                logger.error(f"[Workflow] {category} failed: {result}")
                continue
            responses[f"{category}Expert"] = result

        state["agent_responses"] = responses
        logger.info(f"[Workflow] {len(responses)} experts responded")

        return state

    async def aggregation_node(self, state: ConsultationState) -> ConsultationState:
        """3. Response Aggregator: 답변 통합"""
        logger.info("[Workflow] Step 3: Response Aggregation")

        final_response = await self.aggregator.aggregate(
            user_question=state["message"],
            agent_responses=state["agent_responses"]
        )

        state["final_response"] = final_response
        logger.info(f"[Workflow] Final confidence: {final_response.get('confidence', 0)}")

        return state

    def build_graph(self) -> StateGraph:
        """LangGraph 그래프 구축"""
        workflow = StateGraph(ConsultationState)

        # 노드 추가
        workflow.add_node("context_enrichment", self.context_enrichment_node)
        workflow.add_node("intent_routing", self.intent_routing_node)
        workflow.add_node("expert_consultation", self.expert_consultation_node)
        workflow.add_node("aggregation", self.aggregation_node)

        # 엣지 추가 (순차 실행)
        workflow.set_entry_point("context_enrichment")
        workflow.add_edge("context_enrichment", "intent_routing")
        workflow.add_edge("intent_routing", "expert_consultation")
        workflow.add_edge("expert_consultation", "aggregation")
        workflow.add_edge("aggregation", END)

        return workflow.compile()


# Singleton 인스턴스 (SingletonMeta로 보장됨)
# 함수 기반 싱글톤에서 메타클래스 기반으로 변경
def get_consultation_workflow() -> StateGraph:
    """
    Consultation Workflow 인스턴스 반환

    주의: ConsultationWorkflow는 Singleton이므로 항상 같은 인스턴스 반환
    반환값은 컴파일된 LangGraph
    """
    workflow_builder = ConsultationWorkflow()
    return workflow_builder.build_graph()
