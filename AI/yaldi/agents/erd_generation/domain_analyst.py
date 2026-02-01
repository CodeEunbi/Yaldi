"""
Domain Analyst Agent

사용자 요구사항에서 도메인 모델 추출
"""
from agents.base_agent import BaseAgent
from langchain_core.messages import SystemMessage, HumanMessage
from typing import Dict
import json
import logging
from utils.prompt_loader import prompt_loader

logger = logging.getLogger(__name__)


class DomainAnalystAgent(BaseAgent):
    """
    도메인 분석 전문 Agent

    역할:
    - 사용자 입력에서 핵심 도메인 개념 추출
    - 비즈니스 엔티티 식별
    - 도메인 키워드 추출
    """

    def __init__(self):
        # BaseAgent 초기화 (temperature=0.1: 정확한 도메인 분석)
        super().__init__(agent_name="DomainAnalyst", temperature=0.1)

    async def execute(self, **kwargs) -> Dict:
        """
        BaseAgent의 추상 메서드 구현
        analyze 메서드로 위임
        """
        return await self.analyze(
            project_name=kwargs.get("project_name", ""),
            description=kwargs.get("description", ""),
            user_prompt=kwargs.get("user_prompt", "")
        )

    async def analyze(
        self,
        project_name: str,
        description: str,
        user_prompt: str
    ) -> Dict:
        """
        도메인 분석 수행

        Args:
            project_name: 프로젝트명
            description: 프로젝트 설명
            user_prompt: 사용자 AI 초안 요청

        Returns:
            {
                "agent": "DomainAnalyst",
                "analysis": {
                    "core_entities": ["User", "Booking", ...],
                    "relationships": [...],
                    "keywords": ["reservation", "payment", ...],
                    "business_rules": [...]
                },
                "thought": "...",
                "confidence": 0.9
            }
        """
        try:
            self.log_execution_start(project_name=project_name)
            
            # 프롬프트 로드 (이미 포맷팅됨)
            self.logger.debug("Loading prompts...")
            system_prompt = prompt_loader.load(
                "erd_generation/domain_analyst_system"
            )
            user_prompt_template = prompt_loader.load(
                "erd_generation/domain_analyst_user",
                project_name=project_name,
                description=description or "없음",
                user_prompt=user_prompt
            )

            # 메시지 직접 생성 (ChatPromptTemplate 사용 안 함)
            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt_template)
            ]

            self.logger.info("Calling LLM for domain analysis...")
            response = await self.llm.ainvoke(messages)
            self.logger.debug(f"LLM response received (length: {len(response.content)} chars)")

            # BaseAgent의 JSON 파싱 메서드 사용
            self.logger.debug("Parsing LLM response as JSON...")
            analysis = self._parse_json_response(response.content)

            result = {
                "agent": "DomainAnalyst",
                "analysis": analysis,
                "thought": f"'{project_name}' 프로젝트의 도메인 분석 완료. {len(analysis.get('core_entities', []))}개 핵심 엔티티 식별",
                "confidence": 0.9,
                "confidence_score": 0.9
            }

            entities_count = len(analysis.get('core_entities', []))
            relationships_count = len(analysis.get('relationships', []))
            keywords_count = len(analysis.get('keywords', []))
            self.logger.info(f"COMPLETED - Entities: {entities_count}, Relationships: {relationships_count}, Keywords: {keywords_count}")

            self.log_execution_complete(result)
            return result

        except json.JSONDecodeError as e:
            self.logger.error(f"JSON parsing FAILED: {e}")

            # 폴백: 기본 구조 반환
            fallback_result = {
                "agent": "DomainAnalyst",
                "analysis": {
                    "core_entities": ["User"],
                    "relationships": [],
                    "keywords": [kw.lower() for kw in user_prompt.split()[:5]],
                    "business_rules": []
                },
                "thought": "JSON 파싱 실패, 기본 분석 제공",
                "confidence": 0.3,
                "confidence_score": 0.3
            }

            return self._handle_error(
                error=e,
                fallback_message="JSON 파싱 실패",
                fallback_result=fallback_result
            )

        except Exception as e:
            self.log_execution_failed(e)
            raise
