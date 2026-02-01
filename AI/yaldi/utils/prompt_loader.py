"""
프롬프트 파일 로딩 유틸리티

프롬프트를 코드에서 분리하여 관리하기 위한 유틸리티입니다.
prompts/ 디렉토리의 .txt 파일에서 프롬프트를 읽어옵니다.
"""

from pathlib import Path
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)

# prompts 디렉토리 경로
PROMPTS_DIR = Path(__file__).parent.parent / "prompts"


class PromptLoader:
    """프롬프트 파일을 로드하고 템플릿 변수를 주입하는 클래스"""

    def __init__(self):
        self._cache: Dict[str, str] = {}

    def load(self, prompt_name: str, **kwargs: Any) -> str:
        """
        프롬프트 파일을 로드하고 변수를 주입합니다.

        Args:
            prompt_name: 프롬프트 파일명 (.txt 확장자 제외)
            **kwargs: 프롬프트 템플릿에 주입할 변수들

        Returns:
            str: 변수가 주입된 완성된 프롬프트

        Example:
            >>> loader = PromptLoader()
            >>> prompt = loader.load("mock_data_generation",
            ...                      schema_description="...",
            ...                      row_count=50)
        """
        try:
            # 캐시에서 템플릿 가져오기 (없으면 파일에서 읽기)
            template = self._get_template(prompt_name)

            # 변수 주입
            if kwargs:
                prompt = template.format(**kwargs)
            else:
                prompt = template

            logger.debug(f"Loaded prompt: {prompt_name}")
            return prompt

        except KeyError as e:
            logger.error(f"Missing template variable in {prompt_name}: {e}")
            raise ValueError(f"프롬프트 템플릿에 필요한 변수가 누락되었습니다: {e}")

        except Exception as e:
            logger.error(f"Failed to load prompt {prompt_name}: {e}")
            raise

    def _get_template(self, prompt_name: str) -> str:
        """
        프롬프트 템플릿을 캐시 또는 파일에서 가져옵니다.

        Args:
            prompt_name: 프롬프트 파일명 (.txt 확장자 제외)

        Returns:
            str: 프롬프트 템플릿 문자열
        """
        # 캐시 확인
        if prompt_name in self._cache:
            return self._cache[prompt_name]

        # 파일에서 읽기
        file_path = PROMPTS_DIR / f"{prompt_name}.txt"

        if not file_path.exists():
            raise FileNotFoundError(f"프롬프트 파일을 찾을 수 없습니다: {file_path}")

        with open(file_path, "r", encoding="utf-8") as f:
            template = f.read()

        # 캐시에 저장
        self._cache[prompt_name] = template

        logger.info(f"Loaded prompt template from file: {file_path}")
        return template

    def clear_cache(self):
        """캐시를 비웁니다. (개발 중 프롬프트 수정 시 유용)"""
        self._cache.clear()
        logger.info("Prompt cache cleared")


# 싱글톤 인스턴스
prompt_loader = PromptLoader()
