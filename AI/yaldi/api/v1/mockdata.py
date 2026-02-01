from fastapi import APIRouter, HTTPException
from models.requests.mockdata_requests import MockDataCreateRequest
from models.responses.mockdata_responses import MockDataCreateResponse
from services.mock_data_service import mock_data_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/version", tags=["MockData"])


@router.post("/mock-data", response_model=MockDataCreateResponse)
async def create_mock_data(request: MockDataCreateRequest) -> MockDataCreateResponse:
    """
    Mock Data 생성 API

    Spring Kafka Consumer에서 호출됨:
    1. 스키마 정보(테이블, 컬럼, 제약조건)와 행 수를 받음
    2. LLM을 사용하여 현실적인 더미 데이터 생성
    3. 제약조건을 준수하는 INSERT 문 생성
    4. Spring에서 SQL 파일로 저장 후 S3 업로드

    제약조건 처리:
    - PRIMARY KEY + AUTO_INCREMENT: INSERT 문에서 컬럼 생략
    - UNIQUE: 중복 없는 값 생성
    - NOT NULL: NULL 아닌 값 생성
    - FOREIGN KEY: 참조 테이블의 기존 값 사용

    데이터 특성:
    - 컬럼명과 타입을 고려한 현실적인 값 생성
    - email, name, age 등 의미 있는 데이터
    """
    try:
        logger.info(f"Mock Data 생성 요청 - 테이블 수: {len(request.schema_data.tables)}, 행 수: {request.row_count}")

        # Mock Data SQL 생성
        sql_statements = await mock_data_service.generate_mock_data(request)

        logger.info(f"Mock Data 생성 완료 - SQL 길이: {len(sql_statements)} bytes")

        return MockDataCreateResponse(sql=sql_statements)

    except Exception as e:
        logger.error(f"Mock Data 생성 중 오류: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
