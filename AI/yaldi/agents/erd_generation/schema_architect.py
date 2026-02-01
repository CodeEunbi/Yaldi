"""
Schema Architect Agent

ERD 스키마 설계 전문 Agent
"""
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
from typing import Dict, List, Optional
from config.settings import settings
import json
import logging
from utils.prompt_loader import prompt_loader

logger = logging.getLogger(__name__)


class SchemaArchitectAgent:
    """
    스키마 설계 전문 Agent

    역할:
    - ERD 스키마 설계
    - 테이블 구조 정의
    - 정규화 적용
    """

    def __init__(self):
        self.llm = ChatOpenAI(
            base_url=settings.GMS_BASE_URL,
            api_key=settings.GMS_API_KEY,
            model=settings.OPENAI_MODEL,
            temperature=0.2
        )

    async def design_schema(
        self,
        domain_analysis: Dict,
        user_requirements: str,
        reference_projects: Optional[List[Dict]] = None
    ) -> Dict:
        """
        ERD 스키마 설계

        Args:
            domain_analysis: Domain Analyst 결과
            user_requirements: 사용자 요구사항
            reference_projects: 참고 프로젝트 (있으면 REFERENCE 모드)

        Returns:
            {
                "agent": "SchemaArchitect",
                "schema": {...},
                "mode": "REFERENCE" or "ZERO_BASE",
                "thought": "...",
                "confidence": 0.85
            }
        """
        if reference_projects and len(reference_projects) > 0:
            return await self._design_from_reference(
                domain_analysis, user_requirements, reference_projects
            )
        else:
            return await self._design_from_scratch(
                domain_analysis, user_requirements
            )

    async def _design_from_reference(
        self,
        domain_analysis: Dict,
        requirements: str,
        reference_projects: List[Dict]
    ) -> Dict:
        """참고 프로젝트 기반 설계"""
        logger.info(f"[SchemaArchitect] Starting REFERENCE mode design (using {len(reference_projects)} reference projects)")
        try:
            system_prompt = prompt_loader.load("erd_generation/schema_architect_reference_system")
            user_prompt = prompt_loader.load(
                "erd_generation/schema_architect_reference_user",
                domain_analysis=json.dumps(domain_analysis, ensure_ascii=False, indent=2),
                reference_projects=json.dumps(reference_projects, ensure_ascii=False, indent=2),
                requirements=requirements
            )

            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt)
            ]

            logger.info("[SchemaArchitect] Calling LLM for REFERENCE mode design...")
            response = await self.llm.ainvoke(messages)

            schema = self._parse_json_response(response.content)

            tables_count = len(schema.get('tables', []))
            relations_count = len(schema.get('relations', []))
            logger.info(f"[SchemaArchitect] REFERENCE mode COMPLETED - Tables: {tables_count}, Relations: {relations_count}")
            
            return {
                "agent": "SchemaArchitect",
                "schema": schema,
                "mode": "REFERENCE",
                "thought": f"참고 프로젝트 {len(reference_projects)}개를 기반으로 스키마 설계 완료",
                "confidence": 0.85
            }

        except Exception as e:
            logger.error(f"[SchemaArchitect] REFERENCE mode FAILED: {type(e).__name__}: {str(e)}", exc_info=True)
            raise

    async def _design_from_scratch(
        self,
        domain_analysis: Dict,
        requirements: str
    ) -> Dict:
        """처음부터 새로 설계"""
        logger.info("[SchemaArchitect] Starting ZERO_BASE mode design (from scratch)")
        try:
            system_prompt = prompt_loader.load("erd_generation/schema_architect_zero_base_system")
            user_prompt = prompt_loader.load(
                "erd_generation/schema_architect_zero_base_user",
                domain_analysis=json.dumps(domain_analysis, ensure_ascii=False, indent=2),
                requirements=requirements
            )

            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt)
            ]

            logger.info("[SchemaArchitect] Calling LLM for ZERO_BASE mode design...")
            response = await self.llm.ainvoke(messages)

            schema = self._parse_json_response(response.content)

            tables_count = len(schema.get('tables', []))
            relations_count = len(schema.get('relations', []))
            logger.info(f"[SchemaArchitect] ZERO_BASE mode COMPLETED - Tables: {tables_count}, Relations: {relations_count}")
            
            return {
                "agent": "SchemaArchitect",
                "schema": schema,
                "mode": "ZERO_BASE",
                "thought": "도메인 분석을 바탕으로 완전히 새로운 스키마 설계 완료",
                "confidence": 0.75
            }

        except Exception as e:
            logger.error(f"[SchemaArchitect] ZERO_BASE mode FAILED: {type(e).__name__}: {str(e)}", exc_info=True)
            raise

    def _parse_json_response(self, content: str) -> Dict:
        """LLM 응답에서 JSON 파싱"""
        try:
            # 마크다운 코드 블록 제거
            if "```json" in content:
                content = content.split("```json")[1].split("```")[0].strip()
            elif "```" in content:
                content = content.split("```")[1].split("```")[0].strip()

            return json.loads(content)

        except json.JSONDecodeError as e:
            logger.error(f"[SchemaArchitect] JSON parsing FAILED: {e}")
            logger.error(f"[SchemaArchitect] Content (first 500 chars): {content[:500]}")
            # 폴백: 기본 구조
            return {
                "tables": [
                    {
                        "name": "users",
                        "columns": [
                            {"name": "id", "type": "BIGSERIAL", "constraints": ["PRIMARY KEY"]},
                            {"name": "email", "type": "VARCHAR(255)", "constraints": ["UNIQUE", "NOT NULL"]},
                            {"name": "created_at", "type": "TIMESTAMP", "constraints": ["DEFAULT NOW()"]}
                        ],
                        "indexes": ["idx_users_email"]
                    }
                ],
                "relations": []
            }
