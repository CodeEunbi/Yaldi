from pydantic import BaseModel, Field
from typing import List, Literal, Optional


class VersionVerificationResponse(BaseModel):
    """버전 빌드 검증 응답 (Spring VersionVerificationResult 형식)"""

    is_valid: bool = Field(..., alias="isValid", description="검증 성공 여부")
    status: Literal["SUCCESS", "FAILED"] = Field(..., description="검증 상태")
    errors: Optional[List[str]] = Field(None, description="에러 목록")
    warnings: Optional[List[str]] = Field(None, description="경고 목록")
    message: str = Field(..., description="검증 메시지")
    suggestions: Optional[List[str]] = Field(None, description="수정 제안 목록")

    class Config:
        populate_by_name = True
