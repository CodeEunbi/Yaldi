"""
Context Enrichment Agent

대화 히스토리를 참고하여 애매한 질문을 명확하게 재구성
"""
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
from typing import List, Dict
from config.settings import settings
import json
import logging

logger = logging.getLogger(__name__)


class ContextEnrichmentAgent:
    """
    맥락 보강 Agent

    역할:
    - "이거 해줘", "어 맞아" 같은 짧은 지시어 해석
    - 대화 히스토리에서 맥락 추출
    - 명확한 질문으로 재구성
    """

    # 짧은 질문 패턴 (재구성 필요)
    SHORT_PATTERNS = [
        "어", "응", "네", "예", "그거", "이거", "저거",
        "해줘", "알려줘", "설명해줘", "맞아", "그래"
    ]

    MIN_QUESTION_LENGTH = 10  # 10자 미만이면 재구성 필요

    def __init__(self):
        self.llm = ChatOpenAI(
            base_url=settings.GMS_BASE_URL,
            api_key=settings.GMS_API_KEY,
            model=settings.OPENAI_MODEL,
            temperature=0.0  # deterministic
        )

    async def enrich(
        self,
        user_question: str,
        conversation_history: List[Dict] = None
    ) -> str:
        """
        질문 재구성

        Args:
            user_question: 원래 질문
            conversation_history: 전체 대화 히스토리

        Returns:
            재구성된 질문 (명확하면 원본 그대로)
        """
        try:
            # 충분히 명확하면 재구성 안 함
            if not self._needs_enrichment(user_question, conversation_history):
                logger.info(f"[ContextEnrichment] Question is clear, no enrichment needed")
                return user_question

            logger.info(f"[ContextEnrichment] Enriching question: {user_question}")

            # 대화 히스토리가 없으면 재구성 불가
            if not conversation_history or len(conversation_history) < 2:
                logger.warning("[ContextEnrichment] Not enough history, returning original")
                return user_question

            # LLM으로 재구성
            enriched = await self._reconstruct_question(user_question, conversation_history)

            logger.info(f"[ContextEnrichment] Enriched: {user_question} → {enriched}")
            return enriched

        except Exception as e:
            logger.error(f"[ContextEnrichment] Error: {str(e)}", exc_info=True)
            # 에러 발생 시 원본 반환
            return user_question

    def _needs_enrichment(self, question: str, history: List[Dict]) -> bool:
        """재구성이 필요한지 판단"""
        # 히스토리 없으면 재구성 불가
        if not history or len(history) < 2:
            return False

        # 너무 짧으면 재구성 필요
        if len(question.strip()) < self.MIN_QUESTION_LENGTH:
            return True

        # 짧은 패턴이 포함되어 있으면 재구성 필요
        question_lower = question.lower()
        if any(pattern in question_lower for pattern in self.SHORT_PATTERNS):
            return True

        return False

    async def _reconstruct_question(
        self,
        user_question: str,
        conversation_history: List[Dict]
    ) -> str:
        """LLM으로 질문 재구성"""
        try:
            system_prompt = """당신은 대화 맥락을 파악하여 애매한 질문을 명확하게 재구성하는 전문가입니다.

사용자가 "어 해줘", "그거 맞아" 같은 짧은 표현을 쓰면, 이전 대화를 보고 구체적인 질문으로 바꿔주세요.

규칙:
1. 이전 대화에서 사용자가 물어본 내용 파악
2. 현재 짧은 표현이 무엇을 의미하는지 추론
3. 완전한 문장으로 재구성

JSON 형식:
{
    "enriched_question": "User 테이블 정규화를 진행해주세요",
    "reasoning": "사용자가 이전에 정규화에 대해 물어봤고, '어 해줘'는 그것을 실행하라는 의미"
}

만약 맥락을 파악할 수 없으면 원본 그대로 반환."""

            # 대화 히스토리 텍스트로 구성
            history_text = "=== 대화 히스토리 ===\n"
            for msg in conversation_history[-10:]:  # 최근 10턴
                history_text += f"{msg['role']}: {msg['content']}\n"

            user_prompt = f"""{history_text}

=== 현재 사용자 질문 ===
{user_question}

위 대화 흐름을 보고 현재 질문을 명확하게 재구성하세요.

JSON만 반환:"""

            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt)
            ]

            response = await self.llm.ainvoke(messages)
            result = json.loads(response.content)

            enriched = result.get("enriched_question", user_question)

            # 재구성된 질문이 너무 짧으면 원본 반환
            if len(enriched.strip()) < self.MIN_QUESTION_LENGTH:
                return user_question

            return enriched

        except json.JSONDecodeError as e:
            logger.error(f"[ContextEnrichment] JSON parsing error: {e}")
            return user_question
        except Exception as e:
            logger.error(f"[ContextEnrichment] Reconstruction error: {e}")
            return user_question
