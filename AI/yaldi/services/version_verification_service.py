from models.requests.verification_requests import VersionVerificationRequest
from models.responses.verification_responses import VersionVerificationResponse
from agents.erd_import.sql_validator import sql_validator
from utils.schema_converter import schema_converter
from core.llm.openai_client import openai_client
from typing import Literal
import logging

logger = logging.getLogger(__name__)


class VersionVerificationService:
    """버전 빌드 검증 서비스"""

    async def verify_version(
        self,
        request: VersionVerificationRequest
    ) -> VersionVerificationResponse:
        """
        버전 스키마 검증

        1. SchemaData → SQL 변환
        2. DB 타입 자동 감지
        3. 테스트 DB에서 빌드 검증
        4. 성공: SUCCESS 반환
        5. 실패: LLM으로 분석 후 suggestions 반환
        """
        logger.info(f"버전 검증 시작: {request.version_name}")

        try:
            # 1. SchemaData → SQL 변환
            sql_content = schema_converter.convert_to_sql(request.schema_data)
            logger.info(f"SQL 변환 완료:\n{sql_content}")

            # 2. DB 타입 자동 감지
            db_type = await self._detect_db_type(sql_content)
            logger.info(f"감지된 DB 타입: {db_type}")

            # 3. SQL 빌드 검증
            success, error_message = await sql_validator.validate_sql(
                sql_content=sql_content,
                db_type=db_type
            )

            # 4-A. 성공
            if success:
                logger.info(f"버전 검증 성공: {request.version_name}")
                return VersionVerificationResponse(
                    is_valid=True,
                    status="SUCCESS",
                    errors=None,
                    warnings=None,
                    message="스키마 검증이 완료되었습니다. 문제가 없습니다.",
                    suggestions=None
                )

            # 4-B. 실패 - LLM 분석
            logger.warning(f"버전 검증 실패: {request.version_name}")
            logger.warning(f"DB 에러: {error_message}")

            analysis = await self._analyze_error_with_llm(
                sql_content=sql_content,
                error_message=error_message,
                db_type=db_type
            )

            return VersionVerificationResponse(
                is_valid=False,
                status="FAILED",
                errors=analysis["errors"],
                warnings=analysis.get("warnings"),
                message=analysis["message"],
                suggestions=analysis["suggestions"]
            )

        except Exception as e:
            logger.error(f"버전 검증 중 치명적 오류: {e}", exc_info=True)
            return VersionVerificationResponse(
                is_valid=False,
                status="FAILED",
                errors=[str(e)],
                warnings=None,
                message="스키마 검증 중 오류가 발생했습니다.",
                suggestions=["스키마 데이터를 확인하고 다시 시도해주세요."]
            )

    async def _detect_db_type(self, sql_content: str) -> Literal["postgresql", "mysql"]:
        """LLM을 사용하여 SQL에서 DB 타입 자동 감지"""
        logger.info("LLM으로 DB 타입 자동 감지 중...")

        try:
            prompt = f"""다음 SQL DDL을 분석하여 PostgreSQL인지 MySQL인지 판단하세요.

SQL:
```sql
{sql_content}
```

판단 기준:
- PostgreSQL: SERIAL, BIGSERIAL, TEXT, BOOLEAN 타입 사용
- MySQL: AUTO_INCREMENT, TINYINT, MEDIUMINT 키워드 사용

JSON 형식으로 응답하세요:
{{"dbType": "postgresql" 또는 "mysql"}}
"""

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
                temperature=0.0,
                max_tokens=50
            )

            db_type = response.get("dbType", "postgresql").lower()

            if db_type not in ["postgresql", "mysql"]:
                logger.warning(f"알 수 없는 DB 타입: {db_type}, 기본값(postgresql) 사용")
                db_type = "postgresql"

            return db_type

        except Exception as e:
            logger.error(f"DB 타입 감지 실패: {e}, 기본값(postgresql) 사용")
            return "postgresql"

    async def _analyze_error_with_llm(
        self,
        sql_content: str,
        error_message: str,
        db_type: str
    ) -> dict:
        """
        LLM을 사용하여 에러 분석 및 수정 제안

        Returns:
            {
                "errors": ["에러1", "에러2"],
                "warnings": ["경고1"],
                "message": "사용자 친화적 메시지",
                "suggestions": ["제안1", "제안2"]
            }
        """
        logger.info("LLM으로 에러 분석 중...")

        try:
            prompt = f"""다음 SQL DDL에서 빌드 에러가 발생했습니다. 에러를 분석하고 수정 방법을 제안하세요.

데이터베이스: {db_type.upper()}

SQL:
```sql
{sql_content}
```

에러 메시지:
```
{error_message}
```

다음 형식의 JSON으로 응답하세요:
{{
    "errors": ["구체적인 에러 설명1", "구체적인 에러 설명2"],
    "warnings": ["경고사항1", "경고사항2"],
    "message": "사용자 친화적인 전체 요약 메시지",
    "suggestions": ["구체적인 수정 방법1", "구체적인 수정 방법2"]
}}

요구사항:
- errors: 발생한 에러들을 사용자가 이해하기 쉽게 설명
- warnings: 에러는 아니지만 주의할 점
- message: 전체 상황을 요약한 친절한 메시지
- suggestions: 구체적이고 실행 가능한 수정 방법 제시
"""

            response = await openai_client.json_completion(
                messages=[
                    {
                        "role": "system",
                        "content": "당신은 데이터베이스 전문가입니다. SQL 에러를 분석하고 초보자도 이해할 수 있도록 친절하게 설명합니다."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                temperature=0.3,
                max_tokens=1000
            )

            return {
                "errors": response.get("errors", [error_message]),
                "warnings": response.get("warnings"),
                "message": response.get("message", "SQL 빌드에 실패했습니다."),
                "suggestions": response.get("suggestions", ["스키마를 확인하고 다시 시도해주세요."])
            }

        except Exception as e:
            logger.error(f"LLM 에러 분석 실패: {e}")
            return {
                "errors": [error_message],
                "warnings": None,
                "message": "SQL 빌드에 실패했습니다.",
                "suggestions": ["스키마를 확인하고 다시 시도해주세요."]
            }


version_verification_service = VersionVerificationService()
