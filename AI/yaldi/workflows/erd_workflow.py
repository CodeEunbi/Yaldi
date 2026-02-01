"""
LangGraph 기반 ERD 생성 워크플로우

조건부 분기, Self-Refinement Loop, Multi-Agent 오케스트레이션
"""
from langgraph.graph import StateGraph, END
from typing import TypedDict, Literal, List, Dict, Annotated
import operator
from datetime import datetime
import logging

from agents.erd_generation.domain_analyst import DomainAnalystAgent
from agents.erd_generation.schema_architect import SchemaArchitectAgent
from agents.erd_generation.validator_agent import ValidatorAgent
from agents.erd_generation.optimizer_agent import OptimizerAgent
from agents.erd_generation.meta_agent import MetaAgent
from rag.graph_rag import graph_rag
from services.embedding_service import EmbeddingService
from core.singleton import SingletonMeta
from config.constants import AgentConstants, WorkflowConstants

logger = logging.getLogger(__name__)


# State 정의
class ERDGenerationState(TypedDict):
    # 입력
    project_name: str
    project_description: str
    user_prompt: str

    # 중간 결과
    keywords: List[str]
    similar_projects: List[Dict]
    similarity_score: float

    # Agent 결과들
    domain_analysis: Dict
    schema_design: Dict
    validation_result: Dict
    optimization_result: Dict
    meta_decision: Dict

    # Refinement Loop
    refinement_iteration: int
    issues: Annotated[List[str], operator.add]

    # 최종 결과
    final_schema: Dict
    sql_script: str
    execution_log: Annotated[List[Dict], operator.add]
    mode: Literal["REFERENCE", "ZERO_BASE"]


class ERDGenerationWorkflow(metaclass=SingletonMeta):
    """
    ERD 생성 LangGraph 워크플로우

    플로우:
    1. 도메인 분석 + 키워드 추출
    2. 유사 프로젝트 검색 (Graph RAG)
    3. 스키마 설계 (REFERENCE or ZERO_BASE)
    4. 검증
    5. Refinement Loop (최대 3회)
    6. 최적화
    7. Meta 결정

    Singleton 패턴 적용: 워크플로우 그래프를 한 번만 빌드
    """

    def __init__(self):
        # Singleton이므로 중복 초기화 방지
        if hasattr(self, '_initialized'):
            return
        self._initialized = True

        self.domain_analyst = DomainAnalystAgent()
        self.schema_architect = SchemaArchitectAgent()
        self.validator = ValidatorAgent()
        self.optimizer = OptimizerAgent()
        self.meta_agent = MetaAgent()
        self.graph_rag = graph_rag  # Singleton 인스턴스 사용
        self.embedding_service = EmbeddingService()

        logger.info("ERD Generation Workflow singleton instance created")

    async def domain_analysis_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """1.도메인 분석 + 키워드 추출"""
        logger.info("Step 1: Domain Analysis")

        domain_result = await self.domain_analyst.analyze(
            state["project_name"],
            state.get("project_description", ""),
            state["user_prompt"]
        )

        keywords = domain_result["analysis"].get("keywords", [])

        return {
            **state,
            "keywords": keywords,
            "domain_analysis": domain_result,
            "execution_log": [{
                "step": "domain_analysis",
                "timestamp": datetime.utcnow().isoformat(),
                "result": f"키워드 {len(keywords)}개 추출"
            }]
        }

    async def search_similar_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """2. Graph RAG로 유사 프로젝트 검색"""
        logger.info("Step 2: Searching similar projects with Graph RAG")

        keywords = state.get("keywords", [])

        # Graph RAG 검색
        similar_projects = await self.graph_rag.search_similar_patterns(
            keywords=keywords,
            top_k=WorkflowConstants.GRAPH_RAG_TOP_K
        )

        # 유사도 계산 (간단히 매칭된 프로젝트 수로)
        similarity_score = len(similar_projects) / WorkflowConstants.GRAPH_RAG_TOP_K if similar_projects else 0.0

        return {
            **state,
            "similar_projects": similar_projects,
            "similarity_score": similarity_score,
            "execution_log": [{
                "step": "search_similar",
                "timestamp": datetime.utcnow().isoformat(),
                "result": f"유사 프로젝트 {len(similar_projects)}개 발견, 유사도: {similarity_score:.2%}"
            }]
        }

    def decide_mode(self, state: ERDGenerationState) -> Literal["reference", "zero_base"]:
        """3. 모드 결정 (조건부 분기)"""
        if state["similarity_score"] >= AgentConstants.SIMILARITY_THRESHOLD:
            logger.info(f"Mode: REFERENCE (유사 프로젝트 기반, 유사도: {state['similarity_score']:.2%} >= {AgentConstants.SIMILARITY_THRESHOLD:.2%})")
            return "reference"
        else:
            logger.info(f"Mode: ZERO_BASE (신규 설계, 유사도: {state['similarity_score']:.2%} < {AgentConstants.SIMILARITY_THRESHOLD:.2%})")
            return "zero_base"

    async def design_with_reference_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """4-A REFERENCE 모드 스키마 설계"""
        logger.info("Step 4-A: Schema Design (REFERENCE mode)")

        schema_result = await self.schema_architect.design_schema(
            domain_analysis=state["domain_analysis"],
            user_requirements=state["user_prompt"],
            reference_projects=state["similar_projects"]
        )

        return {
            **state,
            "schema_design": schema_result,
            "mode": "REFERENCE",
            "refinement_iteration": 0,
            "execution_log": [{
                "step": "design_with_reference",
                "timestamp": datetime.utcnow().isoformat(),
                "result": "참고 프로젝트 기반 스키마 설계 완료"
            }]
        }

    async def design_from_scratch_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """4-B ZERO_BASE 모드 스키마 설계"""
        logger.info("Step 4-B: Schema Design (ZERO_BASE mode)")

        schema_result = await self.schema_architect.design_schema(
            domain_analysis=state["domain_analysis"],
            user_requirements=state["user_prompt"],
            reference_projects=None
        )

        return {
            **state,
            "schema_design": schema_result,
            "mode": "ZERO_BASE",
            "refinement_iteration": 0,
            "execution_log": [{
                "step": "design_from_scratch",
                "timestamp": datetime.utcnow().isoformat(),
                "result": "신규 스키마 설계 완료"
            }]
        }

    async def validate_schema_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """5 스키마 검증 (Agentic Tool Use)"""
        iteration = state.get("refinement_iteration", 0)
        logger.info(f"Step 5: Schema Validation (iteration {iteration + 1})")

        validation = await self.validator.validate(
            state["schema_design"]["schema"]
        )

        return {
            **state,
            "validation_result": validation,
            "issues": validation.get("issues", []),
            "execution_log": [{
                "step": "validate_schema",
                "timestamp": datetime.utcnow().isoformat(),
                "result": f"검증 완료. 문제 {len(validation.get('issues', []))}개 발견"
            }]
        }

    def check_validation(self, state: ERDGenerationState) -> Literal["refine", "optimize"]:
        """6  검증 결과 확인 (분기)"""
        current_iteration = state.get("refinement_iteration", 0)
        issues = state.get("issues", [])

        if len(issues) > 0 and current_iteration < AgentConstants.MAX_REFINEMENT_ITERATIONS:
            logger.info(f"Validation failed, refining... (iteration {current_iteration + 1}/{AgentConstants.MAX_REFINEMENT_ITERATIONS})")
            return "refine"
        else:
            if len(issues) > 0:
                logger.warning(f"Max iterations ({AgentConstants.MAX_REFINEMENT_ITERATIONS}) reached with {len(issues)} issues remaining")
            else:
                logger.info("Validation passed!")
            return "optimize"

    async def refine_schema_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """7  스키마 개선 (Self-Refinement Loop)"""
        iteration = state.get("refinement_iteration", 0)
        logger.info(f"Step 7: Schema Refinement (iteration {iteration + 1})")

        # Schema Architect에게 문제점 전달하고 재설계 요청
        # 간단한 재설계: 현재는 다시 design 호출
        refined = await self.schema_architect.design_schema(
            domain_analysis=state["domain_analysis"],
            user_requirements=f"{state['user_prompt']}\n\n수정 필요: {', '.join(state.get('issues', []))}",
            reference_projects=state.get("similar_projects")
        )

        return {
            **state,
            "schema_design": refined,
            "refinement_iteration": iteration + 1,
            "issues": [],  # 초기화
            "execution_log": [{
                "step": "refine_schema",
                "timestamp": datetime.utcnow().isoformat(),
                "result": f"개선 완료 (반복 {iteration + 1}회)"
            }]
        }

    async def optimize_schema_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """8  스키마 최적화"""
        logger.info("Step 8: Schema Optimization")

        optimization = await self.optimizer.optimize(
            schema=state["schema_design"]["schema"],
            domain_analysis=state["domain_analysis"]
        )

        return {
            **state,
            "optimization_result": optimization,
            "execution_log": [{
                "step": "optimize_schema",
                "timestamp": datetime.utcnow().isoformat(),
                "result": "최적화 완료"
            }]
        }

    async def meta_decision_node(self, state: ERDGenerationState) -> ERDGenerationState:
        """9  Meta-Agent 최종 결정"""
        logger.info("Step 9: Meta-Agent Final Decision")

        decision = await self.meta_agent.orchestrate(
            domain_result=state["domain_analysis"],
            schema_result=state["schema_design"],
            validation_result=state["validation_result"],
            optimization_result=state["optimization_result"]
        )

        return {
            **state,
            "meta_decision": decision,
            "final_schema": decision["final_decision"]["final_schema"],
            "sql_script": decision["sql_script"],
            "execution_log": [{
                "step": "meta_decision",
                "timestamp": datetime.utcnow().isoformat(),
                "result": "Meta-Agent 최종 결정 완료"
            }]
        }

    def build_graph(self) -> StateGraph:
        """LangGraph 구축"""
        logger.info("Building LangGraph workflow...")

        workflow = StateGraph(ERDGenerationState)

        # 노드 추가
        workflow.add_node("analyze_domain", self.domain_analysis_node)
        workflow.add_node("search_similar", self.search_similar_node)
        workflow.add_node("design_with_reference", self.design_with_reference_node)
        workflow.add_node("design_from_scratch", self.design_from_scratch_node)
        workflow.add_node("validate_schema", self.validate_schema_node)
        workflow.add_node("refine_schema", self.refine_schema_node)
        workflow.add_node("optimize_schema", self.optimize_schema_node)
        workflow.add_node("finalize_decision", self.meta_decision_node)

        # 엣지 연결
        workflow.set_entry_point("analyze_domain")
        workflow.add_edge("analyze_domain", "search_similar")

        # 조건부 분기 1: 모드 결정
        workflow.add_conditional_edges(
            "search_similar",
            self.decide_mode,
            {
                "reference": "design_with_reference",
                "zero_base": "design_from_scratch"
            }
        )

        workflow.add_edge("design_with_reference", "validate_schema")
        workflow.add_edge("design_from_scratch", "validate_schema")

        # 조건부 분기 2: Refinement Loop
        workflow.add_conditional_edges(
            "validate_schema",
            self.check_validation,
            {
                "refine": "refine_schema",
                "optimize": "optimize_schema"
            }
        )

        workflow.add_edge("refine_schema", "validate_schema")  # 루프!
        workflow.add_edge("optimize_schema", "finalize_decision")
        workflow.add_edge("finalize_decision", END)

        logger.info("LangGraph workflow built successfully")
        return workflow.compile()


# Singleton 인스턴스 (SingletonMeta로 보장됨)
# 함수 기반 싱글톤에서 메타클래스 기반으로 변경
def get_erd_workflow():
    """
    ERD Workflow 인스턴스 반환

    주의: ERDGenerationWorkflow는 Singleton이므로 항상 같은 인스턴스 반환
    반환값은 컴파일된 LangGraph
    """
    workflow_engine = ERDGenerationWorkflow()
    return workflow_engine.build_graph()
