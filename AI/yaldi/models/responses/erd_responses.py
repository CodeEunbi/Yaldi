from pydantic import BaseModel, Field
from typing import Literal, Optional, List
from datetime import datetime


class ColumnSchema(BaseModel):
    """컬럼 스키마"""
    name: str
    type: str
    constraints: Optional[List[str]] = None


class TableSchema(BaseModel):
    """테이블 스키마"""
    name: str
    columns: List[ColumnSchema]


class Schema(BaseModel):
    """전체 스키마"""
    tables: List[TableSchema]


class Suggestion(BaseModel):
    """AI 수정 제안"""
    type: str  # "rename_column", "change_type", "add_constraint", etc.
    table: str
    original: Optional[str] = None
    suggested: str
    reason: str


class ValidationResult(BaseModel):
    """검증 결과 상세"""
    original_error: Optional[str] = Field(None, alias="originalError", description="원본 오류 메시지")
    user_friendly_message: str = Field(..., alias="userFriendlyMessage", description="사용자 친화적 메시지")
    corrected_schema: Optional[Schema] = Field(None, alias="correctedSchema", description="수정된 스키마")
    suggestions: Optional[List[Suggestion]] = Field(None, description="수정 제안 목록")

    class Config:
        populate_by_name = True


class ImportValidationResponse(BaseModel):
    """Spring 서버로 반환하는 Import Validation 응답"""

    request_id: str = Field(..., alias="requestId", description="원본 요청 ID")
    status: Literal["success", "error", "fatal"] = Field(..., description="처리 상태")
    has_errors: bool = Field(..., alias="hasErrors", description="오류 존재 여부")
    processed_at: datetime = Field(default_factory=datetime.utcnow, alias="processedAt", description="처리 완료 시각")
    validation_result: Optional[ValidationResult] = Field(None, alias="validationResult", description="검증 결과")

    class Config:
        populate_by_name = True
