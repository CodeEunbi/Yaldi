from pydantic import BaseModel, Field
from typing import Literal
from datetime import datetime


class ImportValidationRequest(BaseModel):
    """Spring으로부터 넘어오는 Import Validation 요청""" \

    request_id: str = Field(..., alias="requestKey", description="요청 ID")
    user_id: str = Field(..., alias="userKey", description="사용자 ID")
    project_id: str = Field(..., alias="projectKey", description="프로젝트 ID")
    sql_content: str = Field(..., alias="sqlContent", description="검증할 SQL 쿼리")
    timestamp: datetime = Field(..., description="요청 시각")

    class Config:
        populate_by_name = True
