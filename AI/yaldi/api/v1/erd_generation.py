"""
ERD 자동 생성 API

Multi-Agent + Graph RAG + LangGraph
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Literal
from workflows.erd_workflow import get_erd_workflow
import time
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/erd", tags=["ERD Generation"])


# Request/Response 모델
class ERDGenerationRequest(BaseModel):
    project_name: str = Field(..., min_length=1, max_length=200, description="프로젝트명")
    project_description: Optional[str] = Field(None, max_length=1000, description="프로젝트 설명 (선택)")
    user_prompt: str = Field(..., min_length=10, max_length=2000, description="AI 초안 요청 내용")


class AgentThought(BaseModel):
    step: str
    timestamp: str
    result: str


class ERDGenerationResponse(BaseModel):
    mode: Literal["REFERENCE", "ZERO_BASE"]
    similarity_score: float
    similar_projects: List[Dict]
    generated_schema: Dict
    sql_script: str
    explanation: str
    agent_thoughts: List[AgentThought]
    validation_report: Dict
    optimization_suggestions: Dict
    execution_time_ms: int
    confidence_score: float


@router.post("/generate", response_model=ERDGenerationResponse)
async def generate_erd(request: ERDGenerationRequest):
    """
    ERD 자동 생성 API

    사용자는 단순히 3개 텍스트만 입력:
    - project_name
    - project_description (optional)
    - user_prompt

    백엔드에서 모든 AI 기술이 실행됨:
    - Multi-Agent System
    - Graph RAG
    - Agentic Tool Use (실제 DB 검증)
    - Self-Refinement Loop
    - LangGraph Orchestration
    """
    start_time = time.time()

    try:
        logger.info(f"ERD generation started for project: {request.project_name}")

        # LangGraph 워크플로우 실행
        workflow = await get_erd_workflow()

        result = await workflow.ainvoke({
            "project_name": request.project_name,
            "project_description": request.project_description or "",
            "user_prompt": request.user_prompt,
            "execution_log": [],
            "issues": []
        })

        # Agent Thoughts 추출
        agent_thoughts = [
            AgentThought(
                step=log.get("step", "Unknown"),
                timestamp=log.get("timestamp", ""),
                result=log.get("result", "")
            )
            for log in result.get("execution_log", [])
        ]

        # 응답 구성
        execution_time = int((time.time() - start_time) * 1000)

        response = ERDGenerationResponse(
            mode=result.get("mode", "ZERO_BASE"),
            similarity_score=result.get("similarity_score", 0.0),
            similar_projects=result.get("similar_projects", []),
            generated_schema=result.get("final_schema", {}),
            sql_script=result.get("sql_script", ""),
            explanation=result.get("meta_decision", {}).get("final_decision", {}).get("decision_rationale", ""),
            agent_thoughts=agent_thoughts,
            validation_report=result.get("validation_result", {}),
            optimization_suggestions=result.get("optimization_result", {}).get("suggestions", {}),
            execution_time_ms=execution_time,
            confidence_score=result.get("meta_decision", {}).get("final_decision", {}).get("confidence_score", 0.7)
        )

        logger.info(f"ERD generation completed in {execution_time}ms with mode: {response.mode}")
        return response

    except Exception as e:
        logger.error(f"ERD generation failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"ERD 생성 실패: {str(e)}"
        )


@router.get("/health")
async def health_check():
    """Health check for ERD generation service"""
    return {
        "status": "healthy",
        "service": "ERD Generation",
        "features": [
            "Multi-Agent System",
            "Graph RAG",
            "Agentic Tool Use",
            "Self-Refinement Loop",
            "LangGraph Orchestration"
        ]
    }
