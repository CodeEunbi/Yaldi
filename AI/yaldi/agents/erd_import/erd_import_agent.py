from core.llm.openai_client import openai_client
from config.settings import settings
from utils.prompt_loader import prompt_loader
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)


class ERDImportAgent:
    """ERD 도메인 import 전용 AI 에이전트"""

    def __init__(self):
        self.llm = openai_client

    async def analyze_sql_error(
        self,
        sql_content: str,
        error_message: str,
        db_type: str
    ) -> Dict[str, Any]:
        """
        build 후,
        SQL 오류를 분석하고 사용자 친화적인 메시지와 수정안 제시

        Args:
            sql_content: 원본 SQL
            error_message: 데이터베이스 오류 메시지
            db_type: 데이터베이스 타입 (postgresql/mysql)

        Returns:
            {
                "user_friendly_message": str,
                "corrected_sql": str,
                "suggestions": [...]
            }
        """
        logger.info(f"Starting SQL error analysis for {db_type}")

        # 프롬프트 파일에서 로드
        prompt = prompt_loader.load(
            "sql_error_analysis",
            sql_content=sql_content,
            error_message=error_message,
            db_type=db_type
        )

        messages = [
            {"role": "system", "content": "당신은 데이터베이스 전문가입니다."},
            {"role": "user", "content": prompt}
        ]

        result = await self.llm.json_completion(
            messages=messages,
            temperature=settings.ERD_TEMPERATURE,
            max_tokens=settings.ERD_MAX_TOKENS
        )

        logger.info("SQL error analysis completed successfully")
        return result


# Singleton instance
erd_import_agent = ERDImportAgent()
