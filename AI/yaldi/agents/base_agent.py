"""
Base Agent

모든 AI Agent의 공통 기능을 제공하는 추상 클래스
"""
from abc import ABC, abstractmethod
from langchain_openai import ChatOpenAI
from config.settings import settings
from typing import Optional, Dict, Any
import logging
import json


class BaseAgent(ABC):
    """
    모든 AI Agent의 기본 클래스

    공통 기능:
    - LLM 클라이언트 초기화
    - 로깅 설정
    - JSON 파싱 유틸리티
    - 에러 처리 패턴
    """

    def __init__(
        self,
        agent_name: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: Optional[int] = None
    ):
        """
        Args:
            agent_name: Agent 이름 (기본값: 클래스명)
            temperature: LLM 온도 설정 (기본값: 0.7)
            max_tokens: 최대 토큰 수 (기본값: None)
        """
        self.agent_name = agent_name or self.__class__.__name__
        self.temperature = temperature
        self.max_tokens = max_tokens

        # LLM 클라이언트 초기화
        self.llm = self._initialize_llm()

        # 로거 초기화
        self.logger = logging.getLogger(self.agent_name)

        self.logger.debug(f"[{self.agent_name}] initialized with temperature={temperature}")

    def _initialize_llm(self) -> ChatOpenAI:
        """
        LLM 클라이언트 초기화

        Returns:
            ChatOpenAI 인스턴스
        """
        llm_params = {
            "base_url": settings.GMS_BASE_URL,
            "api_key": settings.GMS_API_KEY,
            "model": settings.OPENAI_MODEL,
            "temperature": self.temperature,
        }

        if self.max_tokens is not None:
            llm_params["max_tokens"] = self.max_tokens

        return ChatOpenAI(**llm_params)

    def _parse_json_response(self, content: str) -> Dict[str, Any]:
        """
        LLM 응답에서 JSON 추출 및 파싱

        마크다운 코드 블록(```json ... ```)을 제거하고 JSON 파싱

        Args:
            content: LLM 응답 텍스트

        Returns:
            파싱된 JSON 딕셔너리

        Raises:
            json.JSONDecodeError: JSON 파싱 실패
        """
        content = content.strip()

        # 마크다운 코드 블록 제거
        if "```json" in content:
            content = content.split("```json")[1].split("```")[0].strip()
        elif "```" in content:
            # 첫 번째 코드 블록 추출
            parts = content.split("```")
            if len(parts) >= 3:
                # ``` 로 감싸진 부분 추출
                content = parts[1].strip()
                # 첫 줄이 언어 지시자면 제거 (예: json, python)
                lines = content.split('\n')
                if lines and lines[0].strip().lower() in ['json', 'python', 'javascript']:
                    content = '\n'.join(lines[1:]).strip()

        try:
            return json.loads(content)
        except json.JSONDecodeError as e:
            self.logger.error(f"[{self.agent_name}] JSON parsing failed")
            self.logger.error(f"[{self.agent_name}] Content (first 500 chars): {content[:500]}")
            raise

    def _handle_error(
        self,
        error: Exception,
        fallback_message: str = "처리 중 오류가 발생했습니다",
        fallback_result: Optional[Dict] = None
    ) -> Dict:
        """
        에러 처리 및 폴백 응답 생성

        Args:
            error: 발생한 예외
            fallback_message: 폴백 메시지
            fallback_result: 폴백 결과 (기본값: 에러 메시지만 포함)

        Returns:
            폴백 응답 딕셔너리
        """
        self.logger.error(
            f"[{self.agent_name}] Error: {type(error).__name__}: {str(error)}",
            exc_info=True
        )

        if fallback_result is None:
            fallback_result = {
                "error": True,
                "message": fallback_message,
                "error_type": type(error).__name__,
                "agent": self.agent_name
            }

        return fallback_result

    @abstractmethod
    async def execute(self, **kwargs) -> Dict[str, Any]:
        """
        Agent의 주요 로직 실행 (하위 클래스에서 구현)

        Args:
            **kwargs: Agent별 파라미터

        Returns:
            실행 결과 딕셔너리
        """
        pass

    def log_execution_start(self, **kwargs):
        """실행 시작 로깅"""
        params_summary = ", ".join([f"{k}={str(v)[:50]}" for k, v in kwargs.items()])
        self.logger.info(f"[{self.agent_name}] Execution started - {params_summary}")

    def log_execution_complete(self, result: Dict):
        """실행 완료 로깅"""
        confidence = result.get("confidence", result.get("confidence_score", "N/A"))
        self.logger.info(f"[{self.agent_name}] Execution completed - Confidence: {confidence}")

    def log_execution_failed(self, error: Exception):
        """실행 실패 로깅"""
        self.logger.error(
            f"[{self.agent_name}] Execution FAILED - {type(error).__name__}: {str(error)}"
        )
