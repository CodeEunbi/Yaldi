from pydantic import BaseModel, Field
from typing import List


class VersionEmbeddingResponse(BaseModel):
    """버전 임베딩 생성 응답"""

    vector: List[float] = Field(..., description="임베딩 벡터 (1536차원)")

    class Config:
        populate_by_name = True


class SearchEmbeddingResponse(BaseModel):
    """검색 쿼리 임베딩 생성 응답"""

    query_vector: List[float] = Field(..., alias="queryVector", description="쿼리 임베딩 벡터")
    dimension: int = Field(..., description="벡터 차원")

    class Config:
        populate_by_name = True
