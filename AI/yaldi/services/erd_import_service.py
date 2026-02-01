from models.requests.erd_requests import ImportValidationRequest
from models.responses.erd_responses import (
    ImportValidationResponse,
    ValidationResult,
    Schema,
    Suggestion
)
from agents.erd_import.sql_validator import sql_validator
from agents.erd_import.erd_import_agent import erd_import_agent
from utils.sql_parser import sql_parser
from utils.prompt_loader import prompt_loader
from core.llm.openai_client import openai_client
import logging
from datetime import datetime

logger = logging.getLogger(__name__)


class ERDImportService:
    """ERD 관련 AI 서비스"""

    async def process_import_validation(
        self,
        request: ImportValidationRequest
    ) -> ImportValidationResponse:
        """
        Import Validation 요청 처리

        0. SQL에서 DB 타입 자동 감지
        1. SQL 빌드 검증 (테스트 DB에서 실행)
        2. 오류 발생 시 ERD Agent로 분석 및 수정안 제시
        3. 수정된 스키마 JSON 생성
        4. 응답 반환
        """
        logger.info(f"Processing import validation: {request.request_id}")

        try:
            # 0. LLM으로 DB 타입 자동 감지
            db_type = await self._detect_db_type_from_sql(request.sql_content)
            logger.info(f"🔍 자동 감지된 DB 타입: {db_type}")

            # 1. SQL 빌드 검증
            success, error_message = await sql_validator.validate_sql(
                sql_content=request.sql_content,
                db_type=db_type
            )

            # 2-A. 성공한 경우
            if success:
                logger.info(f"SQL validation successful for request: {request.request_id}")
                schema = sql_parser.parse_sql_to_schema(request.sql_content)

                return ImportValidationResponse(
                    request_id=request.request_id,
                    status="success",
                    has_errors=False,
                    processed_at=datetime.utcnow(),
                    validation_result=ValidationResult(
                        user_friendly_message="SQL 검증이 완료되었습니다. 문제가 없습니다.",
                        corrected_schema=schema,
                        suggestions=None
                    )
                )

            # 2-B. 오류 발생한 경우
            logger.warning(f"SQL validation failed for request: {request.request_id}")
            logger.warning(f"Database error: {error_message}")

            # 3. ERD Agent로 오류 분석
            logger.info(f"에러 수정을 위한 AI 호출 시작")
            ai_result = await erd_import_agent.analyze_sql_error(
                sql_content=request.sql_content,
                error_message=error_message,
                db_type=db_type
            )
            logger.info(f"AI 분석 성공")

            # 4. 수정된 SQL을 스키마로 변환
            corrected_schema = sql_parser.parse_sql_to_schema(ai_result["corrected_sql"])

            # 5. Suggestion 객체로 변환
            suggestions = [
                Suggestion(**suggestion)
                for suggestion in ai_result.get("suggestions", [])
            ]

            return ImportValidationResponse(
                request_id=request.request_id,
                status="error",
                has_errors=True,
                processed_at=datetime.utcnow(),
                validation_result=ValidationResult(
                    original_error=error_message,
                    user_friendly_message=ai_result["user_friendly_message"],
                    corrected_schema=corrected_schema,
                    suggestions=suggestions
                )
            )

        except Exception as e:
            logger.error(f"Import 검사 에러: {e}", exc_info=True)

            return ImportValidationResponse(
                request_id=request.request_id,
                status="fatal",
                has_errors=True,
                processed_at=datetime.utcnow(),
                validation_result=ValidationResult(
                    original_error=str(e),
                    user_friendly_message="Import 에러 검사 중 에러가 발생했습니다",
                    corrected_schema=None,
                    suggestions=None
                )
            )

    async def _detect_db_type_from_sql(self, sql_content: str) -> str:
        """
        LLM을 사용하여 SQL DDL에서 DB 타입 자동 감지

        Args:
            sql_content: CREATE TABLE DDL 등 SQL 원본

        Returns:
            "postgresql" 또는 "mysql"
        """
        logger.info("LLM으로 SQL에서 DB 타입 자동 감지 중...")

        # 프롬프트 생성 (detect_db_type 프롬프트 재사용)
        sql_description = f"SQL DDL:\n```sql\n{sql_content}\n```"
        prompt = prompt_loader.load(
            "detect_db_type",
            schema_description=sql_description
        )

        # LLM 호출 (JSON 응답)
        try:
            response = await openai_client.json_completion(
                messages=[
                    {
                        "role": "system",
                        "content": "당신은 SQL 분석 전문가입니다. SQL DDL을 보고 PostgreSQL인지 MySQL인지 정확히 판단합니다."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                temperature=0.0,  # 결정론적 응답
                max_tokens=50
            )

            db_type = response.get("dbType", "postgresql").lower()

            # 유효성 검증
            if db_type not in ["postgresql", "mysql"]:
                logger.warning(f"알 수 없는 DB 타입: {db_type}, 기본값(postgresql) 사용")
                db_type = "postgresql"

            return db_type

        except Exception as e:
            logger.error(f"DB 타입 감지 실패: {e}, 기본값(postgresql) 사용")
            return "postgresql"


erd_import_service = ERDImportService()
