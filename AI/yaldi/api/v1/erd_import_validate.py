from fastapi import APIRouter, HTTPException
from models.requests.erd_requests import ImportValidationRequest
from models.responses.erd_responses import ImportValidationResponse
from services.erd_import_service import erd_import_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/erd", tags=["ERD"])


@router.post("/validate-import", response_model=ImportValidationResponse)
async def validate_import(request: ImportValidationRequest) -> ImportValidationResponse:
    """
    SQL Import 검증 API

    Spring Consumer에서 호출됨:
    1. SQL을 테스트 DB에서 빌드 검증
    2. 오류 발생 시 OpenAI로 오류에 대한 사용자 친화적 수정안 제시
    3. 수정된 스키마 JSON 반환
    """
    try:
        logger.info(f"SQL Import 검증 API 시작: {request.request_id}")
        logger.info(f" User: {request.user_id}, Project: {request.project_id}")

        response = await erd_import_service.process_import_validation(request)

        logger.info(f"Request {request.request_id} completed - Status: {response.status}, Has Errors: {response.has_errors}")
        return response
    except Exception as e:
        logger.error(f"Error processing import validation: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
