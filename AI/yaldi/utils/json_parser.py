"""
JSON 파싱 유틸리티

LLM이 마크다운 코드 블록으로 감싼 JSON을 파싱
"""
import json
import re
import logging

logger = logging.getLogger(__name__)


def parse_llm_json(content: str) -> dict:
    """
    LLM 응답에서 JSON 추출 및 파싱

    LLM이 다음과 같은 형식으로 응답할 수 있음:
    1. ```json\n{...}\n```
    2. ```\n{...}\n```
    3. {...}

    Args:
        content: LLM 응답 텍스트

    Returns:
        파싱된 JSON 딕셔너리

    Raises:
        json.JSONDecodeError: JSON 파싱 실패
    """
    # 공백 제거
    content = content.strip()

    # 마크다운 코드 블록 제거 (```json ... ``` 또는 ``` ... ```)
    if content.startswith('```'):
        # ```json 또는 ``` 찾기
        lines = content.split('\n')

        # 첫 줄 제거 (```json 또는 ```)
        lines = lines[1:]

        # 마지막 줄이 ```이면 제거
        if lines and lines[-1].strip() == '```':
            lines = lines[:-1]

        content = '\n'.join(lines).strip()

    # JSON 파싱
    try:
        return json.loads(content)
    except json.JSONDecodeError as e:
        logger.error(f"JSON parsing failed. Content: {content[:200]}")
        raise
