from pydantic import BaseModel, Field
from models.schema_models import SchemaData


class MockDataCreateRequest(BaseModel):
    """Mock Data 생성 요청"""
    schema_data: SchemaData = Field(..., alias="schemaData", description="스키마 데이터")
    row_count: int = Field(..., alias="rowCount", ge=1, le=100, description="생성할 행 수 (1-100)")

    class Config:
        populate_by_name = True
