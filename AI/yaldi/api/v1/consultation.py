"""
ERD 상담 챗봇 API

Multi-Agent + Intent Routing + LangGraph
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, List, Dict
import logging
import time

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/consultation", tags=["ERD Consultation"])


# Request/Response 모델
class ConversationMessage(BaseModel):
    role: str = Field(..., pattern="^(user|assistant)$", description="메시지 역할")
    content: str = Field(..., min_length=1, description="메시지 내용")


class ConsultationRequest(BaseModel):
    project_key: int = Field(..., gt=0, description="프로젝트 ID (로깅용)")
    message: str = Field(..., min_length=1, max_length=2000, description="사용자 질문")
    schema_data: Dict = Field(..., description="현재 스키마 데이터 (프론트에서 추출)")
    conversation_history: List[ConversationMessage] = Field(
        default_factory=list,
        max_items=20,
        description="최근 대화 히스토리 (최대 20턴)"
    )


class SchemaModification(BaseModel):
    action: str = Field(..., description="수정 작업 타입 (ADD_INDEX, SPLIT_TABLE 등)")
    description: str = Field(..., description="수정 설명")
    details: Dict = Field(..., description="수정 상세 정보")
    impact: Optional[Dict] = Field(None, description="변경 영향 분석")


class ConsultationResponse(BaseModel):
    message: str = Field(..., description="AI 답변 텍스트")
    schema_modifications: List[Dict] = Field(
        default_factory=list,
        description="적용 가능한 스키마 수정사항"
    )
    confidence: float = Field(..., ge=0.0, le=1.0, description="확신도 (0.0 ~ 1.0)")
    agents_used: List[str] = Field(default_factory=list, description="사용된 Agent 목록")
    warnings: List[str] = Field(default_factory=list, description="경고 사항")


@router.post("/consult", response_model=ConsultationResponse)
async def consult_erd(request: ConsultationRequest):
    """
    ERD 상담 챗봇 API

    사용자 질문에 대해 Multi-Agent System이 답변:
    - Intent Router: 질문 분류
    - Expert Agents: 도메인별 전문 조언 (정규화, PK, 인덱스 등)
    - Response Aggregator: 답변 통합
    - Schema Modifier: 적용 가능한 수정사항 생성

    Args:
        request: 질문 + 스키마 + 대화 히스토리

    Returns:
        AI 답변 + 수정 제안 + 확신도
    """
    try:
        start_time = time.time()

        logger.info(f"[Consultation] ProjectKey={request.project_key}, Message={request.message[:50]}...")

        # LangGraph Workflow 호출
        from workflows.consultation_workflow import get_consultation_workflow

        workflow = get_consultation_workflow()
        result = await workflow.ainvoke({
            "project_key": request.project_key,
            "message": request.message,
            "schema_data": request.schema_data,
            "conversation_history": [msg.dict() for msg in request.conversation_history]
        })

        execution_time = int((time.time() - start_time) * 1000)

        logger.info(f"[Consultation] Completed in {execution_time}ms")

        # 최종 응답 추출
        final_response = result.get("final_response", {})

        return ConsultationResponse(
            message=final_response.get("message", "응답 생성 실패"),
            schema_modifications=final_response.get("schema_modifications", []),
            confidence=final_response.get("confidence", 0.0),
            agents_used=final_response.get("agents_used", []),
            warnings=final_response.get("warnings", [])
        )

    except Exception as e:
        logger.error(f"[Consultation] Error: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"상담 처리 중 오류 발생: {str(e)}")


# Health Check (디버깅용)
@router.get("/health")
async def health_check():
    """상담 챗봇 API 상태 확인"""
    return {
        "status": "ok",
        "service": "erd_consultation",
        "features": [
            "Multi-Agent System",
            "Intent Routing",
            "10 Expert Agents",
            "Schema Modification Suggestions"
        ]
    }
