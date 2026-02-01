from pydantic import BaseModel, Field
from typing import Optional, Dict


class VersionEmbeddingRequest(BaseModel):
    """버전 저장 시 임베딩 생성 요청"""

    version_id: int = Field(..., alias="versionId", description="버전 ID")
    project_id: int = Field(..., alias="projectId", description="프로젝트 ID")

    # 임베딩용 데이터
    project_name: str = Field(..., alias="projectName", description="프로젝트명")
    project_description: Optional[str] = Field(None, alias="projectDescription", description="프로젝트 설명")
    version_name: str = Field(..., alias="versionName", description="버전명")
    version_description: Optional[str] = Field(None, alias="versionDescription", description="버전 설명")
    sql_content: str = Field(..., alias="sqlContent", description="SQL 내용")

    # 가중치 커스터마이징
    weights: Optional[Dict[str, float]] = Field(
        None,
        description="필드별 가중치 (sql, project_name, project_desc, version_name, version_desc)",
        example={
            "sql": 0.5,
            "project_name": 0.25,
            "project_desc": 0.15,
            "version_name": 0.07,
            "version_desc": 0.03
        }
    )

    class Config:
        populate_by_name = True


class SearchEmbeddingRequest(BaseModel):
    """검색 시 쿼리 임베딩 생성 요청"""

    query: str = Field(..., description="검색어")

    class Config:
        populate_by_name = True
