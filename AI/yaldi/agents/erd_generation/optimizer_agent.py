"""
Optimizer Agent

성능 최적화 전문 Agent
"""
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage
from typing import Dict
from config.settings import settings
import json
import logging
from utils.prompt_loader import prompt_loader

logger = logging.getLogger(__name__)


class OptimizerAgent:
    """
    성능 최적화 전문 Agent

    역할:
    - 인덱스 전략 제안
    - 파티셔닝 전략
    - 쿼리 최적화 팁
    - 캐싱 전략
    """

    def __init__(self):
        self.llm = ChatOpenAI(
            base_url=settings.GMS_BASE_URL,
            api_key=settings.GMS_API_KEY,
            model=settings.OPENAI_MODEL,
            temperature=0.3
        )

    async def optimize(self, schema: Dict, domain_analysis: Dict) -> Dict:
        """
        스키마 최적화

        Args:
            schema: 검증된 스키마
            domain_analysis: 도메인 분석 결과

        Returns:
            {
                "agent": "Optimizer",
                "optimized_schema": {...},
                "suggestions": {...},
                "thought": "..."
            }
        """
        try:
            logger.info("[Optimizer] Starting schema optimization analysis...")
            
            system_prompt = prompt_loader.load("erd_generation/optimizer_system")
            user_prompt = prompt_loader.load(
                "erd_generation/optimizer_user",
                schema=json.dumps(schema, ensure_ascii=False, indent=2),
                domain_analysis=json.dumps(domain_analysis, ensure_ascii=False, indent=2)
            )

            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt)
            ]

            logger.info("[Optimizer] Calling LLM for optimization analysis...")
            response = await self.llm.ainvoke(messages)
            logger.debug(f"[Optimizer] LLM response received (length: {len(response.content)} chars)")

            # JSON 파싱
            content = response.content
            if "```json" in content:
                content = content.split("```json")[1].split("```")[0].strip()
            elif "```" in content:
                content = content.split("```")[1].split("```")[0].strip()

            optimization = json.loads(content)

            # 스키마에 최적화 적용
            optimized_schema = self._apply_optimizations(schema, optimization)

            result = {
                "agent": "Optimizer",
                "optimized_schema": optimized_schema,
                "suggestions": optimization,
                "thought": f"총 {len(optimization.get('indexes', []))}개의 인덱스와 {len(optimization.get('query_tips', []))}개의 팁 제안"
            }

            indexes_count = len(optimization.get('indexes', []))
            partitioning_count = len(optimization.get('partitioning', []))
            caching_count = len(optimization.get('caching', []))
            logger.info(f"[Optimizer] COMPLETED - Indexes: {indexes_count}, Partitioning: {partitioning_count}, Caching: {caching_count}")
            return result

        except json.JSONDecodeError as e:
            logger.error(f"[Optimizer] JSON parsing FAILED: {e}")
            # 폴백: 기본 최적화
            return {
                "agent": "Optimizer",
                "optimized_schema": schema,
                "suggestions": {
                    "indexes": [],
                    "partitioning": [],
                    "caching": [],
                    "query_tips": ["기본 인덱스 전략을 따르세요"]
                },
                "thought": "최적화 파싱 실패, 기본 스키마 유지"
            }

        except Exception as e:
            logger.error(f"[Optimizer] FAILED: {type(e).__name__}: {str(e)}", exc_info=True)
            raise

    def _apply_optimizations(self, schema: Dict, optimization: Dict) -> Dict:
        """
        최적화를 스키마에 적용

        Args:
            schema: 원본 스키마
            optimization: 최적화 제안

        Returns:
            최적화가 적용된 스키마
        """
        optimized = json.loads(json.dumps(schema))  # Deep copy

        # 인덱스 추가
        suggested_indexes = optimization.get('indexes', [])
        tables = {t['name']: t for t in optimized.get('tables', [])}

        for idx_suggestion in suggested_indexes:
            table_name = idx_suggestion.get('table', '')
            column = idx_suggestion.get('column', '')
            idx_type = idx_suggestion.get('type', 'btree')

            table = tables.get(table_name)
            if table:
                if 'indexes' not in table:
                    table['indexes'] = []

                idx_name = f"idx_{table_name}_{column}"

                # 중복 체크
                if idx_name not in table['indexes']:
                    table['indexes'].append(idx_name)
                    logger.debug(f"Added index: {idx_name}")

        return optimized
