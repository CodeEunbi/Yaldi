from pydantic import BaseModel, Field
from models.schema_models import SchemaData


class VersionVerificationRequest(BaseModel):
    """버전 빌드 검증 요청"""

    version_name: str = Field(..., alias="versionName", description="버전명")
    schema_data: SchemaData = Field(..., alias="schemaData", description="스키마 데이터")

    class Config:
        populate_by_name = True
