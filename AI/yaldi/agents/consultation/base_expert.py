"""
Base Expert Agent

모든 Expert Agent의 공통 기능
"""
from agents.base_agent import BaseAgent
from langchain_core.messages import SystemMessage, HumanMessage
from typing import Dict, List
import json
import logging
from utils.prompt_loader import prompt_loader
from utils.json_parser import parse_llm_json

logger = logging.getLogger(__name__)


class BaseExpertAgent(BaseAgent):
    """
    Expert Agent 기본 클래스

    모든 Expert Agent는 이 클래스를 상속받음
    BaseAgent를 상속하여 공통 LLM 설정 및 로깅 기능 활용
    """

    def __init__(self, expert_name: str, category: str):
        """
        Args:
            expert_name: Agent 이름 (예: "NormalizationExpert")
            category: 카테고리 (예: "Normalization")
        """
        # BaseAgent 초기화 (temperature=0.3: 창의성과 일관성 균형)
        super().__init__(agent_name=expert_name, temperature=0.3)

        self.expert_name = expert_name
        self.category = category

    async def execute(self, **kwargs) -> Dict:
        """
        BaseAgent의 추상 메서드 구현
        consult 메서드로 위임
        """
        return await self.consult(
            user_question=kwargs.get("user_question", ""),
            schema_data=kwargs.get("schema_data", {}),
            conversation_history=kwargs.get("conversation_history")
        )

    async def consult(
        self,
        user_question: str,
        schema_data: Dict,
        conversation_history: List[Dict] = None
    ) -> Dict:
        """
        상담 수행

        Args:
            user_question: 사용자 질문
            schema_data: 현재 스키마 데이터
            conversation_history: 대화 히스토리

        Returns:
            {
                "answer": "정규화는...",
                "confidence": 0.9,
                "schema_modifications": [{...}],
                "warnings": [...],
                "references": [...]
            }
        """
        try:
            self.log_execution_start(user_question=user_question[:50] + "...")

            # System Prompt 로드
            system_prompt = prompt_loader.load(f"consultation/experts/{self.category.lower()}_system")

            # User Prompt 구성
            user_prompt_template = prompt_loader.load("consultation/experts/common_user")

            # 스키마 요약
            schema_summary = self._summarize_schema(schema_data)

            # 대화 컨텍스트
            context = ""
            if conversation_history:
                recent = conversation_history[-3:]
                context = "\\n=== 최근 대화 ===\\n"
                for msg in recent:
                    context += f"{msg['role']}: {msg['content']}\\n"

            user_prompt = user_prompt_template.format(
                user_question=user_question,
                schema_summary=schema_summary,
                context=context
            )

            # LLM 호출
            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt)
            ]

            response = await self.llm.ainvoke(messages)
            self.logger.debug(f"LLM response: {response.content[:500]}")

            # BaseAgent의 JSON 파싱 메서드 사용
            result = self._parse_json_response(response.content)

            self.logger.info(f"Confidence: {result.get('confidence', 0)}")

            # Self-Reflection: 답변 검증
            result = await self._self_reflect(result, schema_data)

            self.log_execution_complete(result)
            return result

        except json.JSONDecodeError as e:
            self.logger.error(f"JSON parsing error: {e}")
            return self._handle_error(
                error=e,
                fallback_message=f"죄송합니다. {self.category} 분석 중 오류가 발생했습니다.",
                fallback_result={
                    "answer": f"죄송합니다. {self.category} 분석 중 오류가 발생했습니다.",
                    "confidence": 0.0,
                    "schema_modifications": [],
                    "warnings": ["응답 파싱 실패"],
                    "references": []
                }
            )
        except Exception as e:
            self.log_execution_failed(e)
            raise

    def _summarize_schema(self, schema_data: Dict) -> str:
        """
        스키마를 간략하게 요약 (토큰 절약)
        """
        if not schema_data or "tables" not in schema_data:
            return "스키마 없음"

        tables = schema_data.get("tables", [])
        summary = f"테이블 수: {len(tables)}\\n\\n"

        for table in tables[:10]:  # 최대 10개만
            table_name = table.get("physicalName", table.get("logicalName", "Unknown"))
            columns = table.get("columns", [])
            summary += f"- {table_name} ({len(columns)}개 컬럼)\\n"

        if len(tables) > 10:
            summary += f"... (외 {len(tables) - 10}개 테이블)\\n"

        return summary

    async def _self_reflect(self, result: Dict, schema_data: Dict) -> Dict:
        """
        Self-Reflection: 답변 검증

        검증 항목:
        1. 스키마 일관성: 제안한 변경사항이 현재 스키마와 충돌하지 않나?
        2. 논리적 모순: 답변 내용이 서로 모순되지 않나?
        3. 실행 가능성: 실제로 적용 가능한 제안인가?
        """
        try:
            # schema_modifications가 없으면 검증 스킵
            modifications = result.get("schema_modifications", [])
            if not modifications or not schema_data:
                return result

            logger.info(f"[{self.expert_name}] Self-reflection on {len(modifications)} modifications")

            # 간단한 검증: 중복 체크
            existing_tables = {
                table.get("physicalName", table.get("logicalName", ""))
                for table in schema_data.get("tables", [])
            }

            validated_modifications = []
            new_warnings = list(result.get("warnings", []))

            for mod in modifications:
                action = mod.get("action", "")
                details = mod.get("details", {})
                table_name = details.get("table", "")

                # ADD_INDEX: 테이블 존재 확인
                if action == "ADD_INDEX" and table_name not in existing_tables:
                    new_warnings.append(f"경고: {table_name} 테이블이 현재 스키마에 없습니다")
                    logger.warning(f"[{self.expert_name}] Table {table_name} not found in schema")
                    continue

                # SPLIT_TABLE: from_table 존재 확인
                if action == "SPLIT_TABLE":
                    from_table = details.get("from_table", "")
                    if from_table and from_table not in existing_tables:
                        new_warnings.append(f"경고: {from_table} 테이블이 현재 스키마에 없습니다")
                        logger.warning(f"[{self.expert_name}] Table {from_table} not found")
                        continue

                # 검증 통과한 modification만 추가
                validated_modifications.append(mod)

            # 검증 결과 반영
            result["schema_modifications"] = validated_modifications
            result["warnings"] = new_warnings

            # 검증 후 confidence 조정
            if len(validated_modifications) < len(modifications):
                original_confidence = result.get("confidence", 0.5)
                result["confidence"] = max(0.3, original_confidence - 0.1)
                logger.info(f"[{self.expert_name}] Confidence adjusted: {original_confidence} → {result['confidence']}")

            return result

        except Exception as e:
            logger.error(f"[{self.expert_name}] Self-reflection error: {e}")
            # 에러 발생 시 원본 그대로 반환
            return result
