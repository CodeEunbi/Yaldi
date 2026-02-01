from pydantic import BaseModel, Field


class MockDataCreateResponse(BaseModel):
    """Mock Data 생성 응답"""

    sql: str = Field(..., description="생성된 INSERT 문들")

    class Config:
        populate_by_name = True
