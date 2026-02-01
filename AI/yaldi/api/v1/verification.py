from fastapi import APIRouter, HTTPException
from models.requests.verification_requests import VersionVerificationRequest
from models.responses.verification_responses import VersionVerificationResponse
from services.version_verification_service import version_verification_service
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/version", tags=["Verification"])


@router.post("/verification", response_model=VersionVerificationResponse)
async def verify_version(request: VersionVerificationRequest) -> VersionVerificationResponse:
    """
    버전 빌드 검증 API

    Spring Kafka Consumer에서 호출됨:
    1. SchemaData를 SQL DDL로 변환
    2. DB 타입 자동 감지 (PostgreSQL/MySQL)
    3. 테스트 DB에서 빌드 검증
    4. 성공: SUCCESS 반환
    5. 실패: LLM으로 에러 분석 후 수정 제안 반환

    검증 프로세스:
    - 가상 Docker DB에서 실제 CREATE TABLE 실행
    - 문법 오류, 제약조건 충돌 등 감지
    - 실패 시 사용자 친화적인 에러 설명 + 구체적 수정 방법 제시
    """
    try:
        logger.info(f"버전 검증 요청 - 버전명: {request.version_name}, 테이블 수: {len(request.schema_data.tables)}")

        # 버전 검증 수행
        result = await version_verification_service.verify_version(request)

        logger.info(f"버전 검증 완료 - 상태: {result.status}")

        return result

    except Exception as e:
        logger.error(f"버전 검증 중 오류: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
