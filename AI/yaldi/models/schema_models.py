from pydantic import BaseModel, Field, field_validator
from typing import Optional, List


class ColumnSchema(BaseModel):
    """컬럼 스키마 정보 (Spring Version.schemaData 형식)"""
    columnKey: int = Field(..., alias="columnKey", description="컬럼 키")
    physicalName: str = Field(..., alias="physicalName", description="물리 컬럼명")
    logicalName: str = Field(..., alias="logicalName", description="논리 컬럼명")
    dataType: str = Field(..., alias="dataType", description="데이터 타입 (VARCHAR, INT, ENUM 등)")
    dataDetail: List[str] = Field(default_factory=list, alias="dataDetail", description="데이터 상세 (크기, ENUM 값 등)")
    isPrimaryKey: bool = Field(False, alias="isPrimaryKey", description="Primary Key 여부")
    isNullable: bool = Field(True, alias="isNullable", description="NULL 허용 여부")
    isUnique: bool = Field(False, alias="isUnique", description="UNIQUE 제약조건 여부")
    isForeignKey: bool = Field(False, alias="isForeignKey", description="Foreign Key 여부")
    isIncremental: bool = Field(False, alias="isIncremental", description="AUTO_INCREMENT 여부")
    defaultValue: Optional[str] = Field(None, alias="defaultValue", description="기본값")

    @field_validator('dataDetail', mode='before')
    @classmethod
    def convert_data_detail_to_strings(cls, v):
        """dataDetail의 모든 값을 문자열로 변환 (숫자가 들어와도 처리)"""
        if isinstance(v, list):
            return [str(item) if not isinstance(item, str) else item for item in v]
        return v

    class Config:
        populate_by_name = True

class RelationSchema(BaseModel):
    """관계 스키마 정보"""
    fromTableKey: int = Field(..., alias="fromTableKey", description="FROM 테이블 키")
    toTableKey: int = Field(..., alias="toTableKey", description="TO 테이블 키")
    relationType: str = Field(..., alias="relationType", description="관계 타입 (1:1, 1:N, N:1, N:M)")
    constraintName: Optional[str] = Field(None, alias="constraintName", description="제약조건명")
    onDeleteAction: Optional[str] = Field(None, alias="onDeleteAction", description="ON DELETE 동작")
    onUpdateAction: Optional[str] = Field(None, alias="onUpdateAction", description="ON UPDATE 동작")

    class Config:
        populate_by_name = True


class TableSchema(BaseModel):
    """테이블 스키마 정보 (Spring Version.schemaData 형식)"""
    tableKey: int = Field(..., alias="tableKey", description="테이블 키")
    physicalName: str = Field(..., alias="physicalName", description="물리 테이블명")
    logicalName: str = Field(..., alias="logicalName", description="논리 테이블명")
    columns: List[ColumnSchema] = Field(..., description="컬럼 목록")

    class Config:
        populate_by_name = True


class SchemaData(BaseModel):
    """스키마 데이터 (Spring Version.schemaData 형식)"""
    tables: List[TableSchema] = Field(..., description="테이블 목록")
    relations: Optional[List[RelationSchema]] = Field(default_factory=list, description="관계 목록")

    class Config:
        populate_by_name = True
