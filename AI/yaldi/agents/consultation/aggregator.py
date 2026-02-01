"""
Response Aggregator

여러 Expert Agent의 답변을 하나로 통합
"""
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
from typing import Dict, List
from config.settings import settings
import json
import logging
from utils.prompt_loader import prompt_loader
from utils.json_parser import parse_llm_json

logger = logging.getLogger(__name__)


class ResponseAggregator:
    """
    답변 통합 Agent

    역할:
    - 여러 Expert 답변을 하나의 일관된 답변으로 통합
    - 상충되는 조언이 있으면 Trade-off 분석
    - 우선순위 제안
    """

    def __init__(self):
        self.llm = ChatOpenAI(
            base_url=settings.GMS_BASE_URL,
            api_key=settings.GMS_API_KEY,
            model=settings.OPENAI_MODEL,
            temperature=0.2
        )

    async def aggregate(
        self,
        user_question: str,
        agent_responses: Dict[str, Dict]
    ) -> Dict:
        """
        답변 통합

        Args:
            user_question: 원래 질문
            agent_responses: {
                "NormalizationExpert": {...},
                "IndexStrategyExpert": {...}
            }

        Returns:
            {
                "message": "통합된 최종 답변",
                "schema_modifications": [...],
                "confidence": 0.88,
                "agents_used": ["NormalizationExpert", ...],
                "warnings": [...]
            }
        """
        try:
            logger.info(f"[Aggregator] Aggregating {len(agent_responses)} responses")

            # 단일 Agent 응답이면 그대로 반환
            if len(agent_responses) == 1:
                result = self._format_single_response(user_question, agent_responses)
                # 낮은 확신도면 질문 재구성 제안
                if result["confidence"] < 0.5:
                    result = await self._suggest_question_refinement(user_question, result)
                return result

            # 복수 Agent 응답 통합
            result = await self._aggregate_multiple(user_question, agent_responses)

            # 낮은 확신도면 질문 재구성 제안
            if result["confidence"] < 0.5:
                result = await self._suggest_question_refinement(user_question, result)

            return result

        except Exception as e:
            logger.error(f"[Aggregator] Error: {str(e)}", exc_info=True)
            raise

    def _format_single_response(
        self,
        user_question: str,
        agent_responses: Dict[str, Dict]
    ) -> Dict:
        """단일 Agent 응답 포맷"""
        agent_name, response = list(agent_responses.items())[0]

        message = response.get("answer", "")

        # options가 있으면 추가
        options = response.get("options", [])
        if options:
            message += "\n\n### 선택 가능한 옵션\n\n"
            for i, option in enumerate(options, 1):
                message += f"**옵션 {i}: {option.get('title', '')}**\n"
                if option.get('pros'):
                    message += f"- 장점: {', '.join(option['pros'])}\n"
                if option.get('cons'):
                    message += f"- 단점: {', '.join(option['cons'])}\n"
                if option.get('recommendation'):
                    message += f"- 추천: {option['recommendation']}\n"
                message += "\n"

        return {
            "message": message,
            "schema_modifications": response.get("schema_modifications", []),
            "confidence": response.get("confidence", 0.5),
            "agents_used": [agent_name],
            "warnings": response.get("warnings", [])
        }

    async def _aggregate_multiple(
        self,
        user_question: str,
        agent_responses: Dict[str, Dict]
    ) -> Dict:
        """복수 Agent 응답 통합"""
        try:
            # System Prompt
            system_prompt = prompt_loader.load("consultation/aggregator_system")

            # Agent 답변들을 텍스트로 구성
            responses_text = ""
            for agent_name, response in agent_responses.items():
                responses_text += f"\\n[{agent_name}]\\n"
                responses_text += f"답변: {response.get('answer', '')}\\n"
                responses_text += f"확신도: {response.get('confidence', 0)}\\n"
                if response.get('warnings'):
                    responses_text += f"경고: {', '.join(response['warnings'])}\\n"

            # User Prompt
            user_prompt_template = prompt_loader.load("consultation/aggregator_user")
            user_prompt = user_prompt_template.format(
                user_question=user_question,
                responses=responses_text
            )

            # LLM 호출
            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt)
            ]

            response = await self.llm.ainvoke(messages)
            result = parse_llm_json(response.content)

            # schema_modifications 통합
            all_modifications = []
            for agent_response in agent_responses.values():
                all_modifications.extend(agent_response.get("schema_modifications", []))

            # 중복 제거 (같은 action + table + column)
            unique_modifications = self._deduplicate_modifications(all_modifications)

            return {
                "message": result.get("message", ""),
                "schema_modifications": unique_modifications,
                "confidence": result.get("confidence", 0.5),
                "agents_used": list(agent_responses.keys()),
                "warnings": result.get("warnings", [])
            }

        except json.JSONDecodeError as e:
            logger.error(f"[Aggregator] JSON parsing error: {e}")
            # Fallback: 단순 연결
            return self._simple_concat(user_question, agent_responses)

    def _deduplicate_modifications(self, modifications: List[Dict]) -> List[Dict]:
        """중복 수정사항 제거"""
        seen = set()
        unique = []

        for mod in modifications:
            key = (
                mod.get("action"),
                mod.get("details", {}).get("table"),
                mod.get("details", {}).get("column")
            )
            if key not in seen:
                seen.add(key)
                unique.append(mod)

        return unique

    def _simple_concat(
        self,
        user_question: str,
        agent_responses: Dict[str, Dict]
    ) -> Dict:
        """Fallback: 단순 연결"""
        message = ""
        for agent_name, response in agent_responses.items():
            message += f"\\n[{agent_name} 관점]\\n"
            message += response.get("answer", "") + "\\n"

        all_warnings = []
        for response in agent_responses.values():
            all_warnings.extend(response.get("warnings", []))

        return {
            "message": message,
            "schema_modifications": [],
            "confidence": 0.5,
            "agents_used": list(agent_responses.keys()),
            "warnings": list(set(all_warnings))
        }

    async def _suggest_question_refinement(
        self,
        user_question: str,
        current_result: Dict
    ) -> Dict:
        """
        낮은 확신도일 때 질문 재구성 제안

        Meta Agent 역할: 질문을 더 명확하게 만들기 위한 제안
        """
        try:
            logger.info("[Aggregator] Low confidence, suggesting question refinement")

            system_prompt = """당신은 사용자의 불명확한 질문을 더 구체적으로 만드는 도우미입니다.

사용자 질문이 애매하거나 너무 광범위할 때, 다음과 같이 도와주세요:
1. 질문이 왜 불명확한지 설명
2. 구체적인 질문 예시 2-3개 제시

JSON 형식:
{
    "refined_suggestions": [
        "User 테이블 정규화 방법이 궁금하신가요?",
        "PK 선택 기준을 알고 싶으신가요?"
    ],
    "reason": "질문이 너무 광범위하여 구체적인 답변이 어렵습니다."
}"""

            user_prompt = f"""원래 질문: {user_question}

현재 답변: {current_result.get('message', '')}
확신도: {current_result.get('confidence', 0)}

이 질문을 더 구체적으로 만들어주세요."""

            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt)
            ]

            response = await self.llm.ainvoke(messages)
            suggestions = parse_llm_json(response.content)

            # 기존 답변에 재구성 제안 추가
            refinement_text = f"\n\n💡 **질문을 더 구체적으로 바꿔보시겠어요?**\n\n"
            refinement_text += f"{suggestions.get('reason', '')}\n\n"
            refinement_text += "**추천 질문:**\n"
            for i, suggestion in enumerate(suggestions.get('refined_suggestions', []), 1):
                refinement_text += f"{i}. {suggestion}\n"

            current_result["message"] += refinement_text
            current_result["warnings"].append("질문이 불명확하여 재구성 제안 포함")

            return current_result

        except Exception as e:
            logger.error(f"[Aggregator] Question refinement error: {e}")
            # 실패해도 기존 결과 반환
            return current_result
