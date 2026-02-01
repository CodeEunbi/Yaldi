"""
Graph RAG 인덱싱 API

Spring Boot에서 호출하여 Neo4j에 프로젝트 스키마 인덱싱
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Dict, Optional
from rag.graph_rag import get_graph_rag
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/graph-rag", tags=["Graph RAG Indexing"])


class GraphIndexingRequest(BaseModel):
    version_key: int = Field(..., description="버전 키")
    version_name: str = Field(..., description="버전 이름")
    version_description: str = Field(default="", description="버전 설명")
    project_name: str = Field(..., description="프로젝트 이름")
    project_description: str = Field(default="", description="프로젝트 설명")
    schema_data: Dict = Field(..., description="스키마 데이터 (tables, relations 포함)")
    is_public: bool = Field(default=True, description="공개 여부")
    design_verification_status: str = Field(default="SUCCESS", description="검증 상태")


class GraphIndexingResponse(BaseModel):
    success: bool
    message: str
    version_key: int


@router.post("/index", response_model=GraphIndexingResponse)
async def index_to_graph(request: GraphIndexingRequest):
    """
    프로젝트 스키마를 Neo4j Graph에 인덱싱

    Spring Boot에서 빌드 성공 시 호출
    """
    try:
        logger.info(f"Graph indexing requested - version_key: {request.version_key}, project: {request.project_name}")

        # Graph RAG 인스턴스 가져오기
        graph_rag = await get_graph_rag()

        # Neo4j에 인덱싱
        await graph_rag.index_project_schema(
            version_key=request.version_key,
            version_name=request.version_name,
            version_description=request.version_description,
            project_name=request.project_name,
            project_description=request.project_description,
            schema_data=request.schema_data,
            is_public=request.is_public,
            design_verification_status=request.design_verification_status
        )

        logger.info(f"Graph indexing completed - version_key: {request.version_key}")

        return GraphIndexingResponse(
            success=True,
            message=f"프로젝트가 Neo4j에 성공적으로 인덱싱되었습니다",
            version_key=request.version_key
        )

    except Exception as e:
        logger.error(f"Graph indexing failed: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Graph 인덱싱 실패: {str(e)}"
        )


@router.get("/health")
async def health_check():
    """Graph RAG health check"""
    try:
        graph_rag = await get_graph_rag()
        is_healthy = await graph_rag.health_check()

        return {
            "status": "healthy" if is_healthy else "unhealthy",
            "neo4j_connected": is_healthy
        }
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return {
            "status": "unhealthy",
            "neo4j_connected": False,
            "error": str(e)
        }
