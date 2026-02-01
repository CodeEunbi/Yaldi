from models.requests.mockdata_requests import MockDataCreateRequest
from agents.mock_data import mock_data_agent
import logging

logger = logging.getLogger(__name__)


class MockDataService:
    """Mock Data 생성 서비스 (Agent 위임)"""

    async def generate_mock_data(self, request: MockDataCreateRequest) -> str:
        """
        스키마 정보를 기반으로 Mock Data INSERT 문 생성

        내부적으로 MockDataAgent를 사용하여:
        1. LLM으로 INSERT 문 생성
        2. SQLite in-memory DB에서 검증
        3. 에러 발생 시 자동 수정 (최대 3회)

        Args:
            request: Mock Data 생성 요청 (스키마 정보, 행 수)

        Returns:
            str: 검증 완료된 INSERT SQL 문들
        """
        try:
            # Agent에게 위임
            sql_statements = await mock_data_agent.generate_mock_data(request)
            return sql_statements

        except Exception as e:
            logger.error(f"Mock Data 생성 중 오류 발생: {e}", exc_info=True)
            raise



# 인스턴스 생성 (필요시 여러 개 생성 가능, Service는 상태를 유지하지 않음)
# 일반적으로 하나의 인스턴스만 사용
mock_data_service = MockDataService()
