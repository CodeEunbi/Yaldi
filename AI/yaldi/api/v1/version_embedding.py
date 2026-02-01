from fastapi import APIRouter, HTTPException
from models.requests.embedding_requests import VersionEmbeddingRequest
from models.responses.embedding_responses import VersionEmbeddingResponse
from core.llm.openai_client import openai_client
from services.embedding_service import embedding_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/version", tags=["Embedding"])


@router.post("/embedding", response_model=VersionEmbeddingResponse)
async def create_version_embedding(request: VersionEmbeddingRequest) -> VersionEmbeddingResponse:
    """
    버전 저장 시 가중치 기반 임베딩 생성 API

    Spring에서 버전 생성 시 호출됨:
    1. 각 필드(프로젝트명, 설명, 버전명, SQL)를 개별 임베딩
    2. 가중치를 적용하여 합성
    3. 생성된 벡터를 Spring으로 반환
    4. Spring에서 PostgreSQL vector 컬럼에 저장

    가중치 커스터마이징:
    - weights 파라미터로 각 필드의 중요도 조정 가능
    - 기본값: {"sql": 0.5, "project_name": 0.25, "project_desc": 0.15,
                "version_name": 0.07, "version_desc": 0.03}
    """
    try:
        logger.info(f"버전 임베딩 생성 요청 - VersionId: {request.version_id}, ProjectId: {request.project_id}")

        # 필드 데이터 준비
        fields = {
            "project_name": request.project_name,
            "project_desc": request.project_description,
            "version_name": request.version_name,
            "version_desc": request.version_description,
            "sql": request.sql_content
        }

        # 가중치 기반 임베딩 생성
        vector = await embedding_service.create_weighted_embedding(
            fields=fields,
            weights=request.weights
        )

        logger.info(f"버전 임베딩 생성 완료 - VersionId: {request.version_id}, Dimension: {len(vector)}")

        return VersionEmbeddingResponse(
            vector=vector
        )

    except Exception as e:
        logger.error(f"버전 임베딩 생성 중 오류: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
