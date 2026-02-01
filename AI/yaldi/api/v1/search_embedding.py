from fastapi import APIRouter, HTTPException
from models.requests.embedding_requests import SearchEmbeddingRequest
from models.responses.embedding_responses import SearchEmbeddingResponse
from core.llm.openai_client import openai_client
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/search", tags=["Embedding"])

@router.post("/embedding", response_model=SearchEmbeddingResponse)
async def create_search_embedding(request: SearchEmbeddingRequest) -> SearchEmbeddingResponse:
    """
    클라이언트 검색 시 쿼리 임베딩 생성 API

    Spring에서 검색 요청 시 호출됨:
    1. 클라이언트 검색어를 임베딩으로 변환
    2. Spring에서 Elasticsearch knn 검색에 사용
    """
    try:
        logger.info(f"검색 쿼리 임베딩 생성 요청 - Query: {request.query}")

        # 임베딩 생성
        query_vector = await openai_client.create_embedding(request.query)

        logger.info(f"검색 쿼리 임베딩 생성 완료 - Dimension: {len(query_vector)}")

        return SearchEmbeddingResponse(
            query_vector=query_vector,
            dimension=len(query_vector)
        )

    except Exception as e:
        logger.error(f"검색 쿼리 임베딩 생성 중 오류: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
