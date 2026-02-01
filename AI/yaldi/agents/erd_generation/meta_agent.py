"""
Meta-Agent

모든 Agent의 결과를 종합하여 최종 의사결정
"""
from agents.base_agent import BaseAgent
from langchain_core.prompts import ChatPromptTemplate
from typing import Dict
import json
import logging
from utils.prompt_loader import prompt_loader

logger = logging.getLogger(__name__)


class MetaAgent(BaseAgent):
    """
    Meta-Agent: 최종 의사결정권자

    역할:
    - Agent 실행 결과 통합
    - 최종 스키마 결정
    - 품질 평가
    - SQL 생성
    """

    def __init__(self):
        # BaseAgent 초기화 (temperature=0.1: 신중한 의사결정)
        super().__init__(agent_name="MetaAgent", temperature=0.1)

    async def execute(self, **kwargs) -> Dict:
        """
        BaseAgent의 추상 메서드 구현
        orchestrate 메서드로 위임
        """
        return await self.orchestrate(
            domain_result=kwargs.get("domain_result", {}),
            schema_result=kwargs.get("schema_result", {}),
            validation_result=kwargs.get("validation_result", {}),
            optimization_result=kwargs.get("optimization_result", {})
        )

    async def orchestrate(
        self,
        domain_result: Dict,
        schema_result: Dict,
        validation_result: Dict,
        optimization_result: Dict
    ) -> Dict:
        """
        모든 Agent 결과를 통합하고 최종 결정

        Args:
            domain_result: Domain Analyst 결과
            schema_result: Schema Architect 결과
            validation_result: Validator 결과
            optimization_result: Optimizer 결과

        Returns:
            {
                "agent": "MetaAgent",
                "final_decision": {
                    "final_schema": {...},
                    "confidence_score": 0.92,
                    "decision_rationale": "...",
                    "requires_human_review": false,
                    "warnings": [...]
                },
                "sql_script": "...",
                "thought": "..."
            }
        """
        try:
            self.log_execution_start()

            # 검증 실패 시 경고
            self.logger.info("Starting final decision orchestration...")
            if not validation_result.get('is_valid', False):
                self.logger.warning(f"Validation issues detected: {validation_result.get('issues', [])}")

            # 최적화된 스키마 사용
            final_schema = optimization_result.get('optimized_schema', schema_result.get('schema', {}))

            # LLM에게 최종 평가 요청
            prompt = ChatPromptTemplate.from_messages([
                ("system", """당신은 최고 의사결정권자(Meta-Agent)입니다.

각 전문 Agent의 결과를 종합하여 최종 ERD를 결정하세요.

고려사항:
1. 도메인 분석이 요구사항을 잘 반영했는가?
2. 스키마 설계가 합리적인가?
3. 검증 결과에 치명적 문제는 없는가?
4. 최적화가 적절히 적용되었는가?

JSON 형식으로 최종 결정 반환:
{{
    "confidence_score": 0.92,
    "decision_rationale": "도메인 분석이 정확하고, 스키마 설계가 3차 정규화를 준수하며, 모든 검증을 통과했습니다.",
    "requires_human_review": false,
    "warnings": ["테이블 수가 많아 복잡도가 높을 수 있습니다"]
}}
"""),
                ("user", """
=== Domain Analysis ===
{domain}

=== Schema Design ===
Mode: {mode}
{schema}

=== Validation ===
Valid: {is_valid}
Issues: {issues}

=== Optimization ===
{optimization}

위 결과들을 종합하여 최종 ERD의 품질을 평가하세요. JSON만 반환하세요.
""")
            ])

            response = await self.llm.ainvoke(
                prompt.format_messages(
                    domain=json.dumps(domain_result.get('analysis', {}), ensure_ascii=False, indent=2),
                    mode=schema_result.get('mode', 'UNKNOWN'),
                    schema=json.dumps(schema_result.get('schema', {}), ensure_ascii=False, indent=2),
                    is_valid=validation_result.get('is_valid', False),
                    issues=json.dumps(validation_result.get('issues', []), ensure_ascii=False),
                    optimization=json.dumps(optimization_result.get('suggestions', {}), ensure_ascii=False, indent=2)
                )
            )

            # BaseAgent의 JSON 파싱 메서드 사용
            decision = self._parse_json_response(response.content)

            # SQL 스크립트 생성
            sql_script = self._generate_sql(final_schema)

            result = {
                "agent": "MetaAgent",
                "final_decision": {
                    **decision,
                    "final_schema": final_schema
                },
                "sql_script": sql_script,
                "thought": "모든 Agent 결과를 종합하여 최종 ERD를 결정했습니다",
                "confidence_score": decision.get('confidence_score', 0)
            }

            confidence = decision.get('confidence_score', 0)
            requires_review = decision.get('requires_human_review', False)
            warnings = decision.get('warnings', [])
            self.logger.info(f"COMPLETED - Confidence: {confidence:.2f}, Requires Review: {requires_review}, Warnings: {len(warnings)}")
            if warnings:
                self.logger.warning(f"Warnings: {warnings}")

            self.log_execution_complete(result)
            return result

        except json.JSONDecodeError as e:
            self.logger.error(f"JSON parsing FAILED: {e}")

            # 폴백
            fallback_result = {
                "agent": "MetaAgent",
                "final_decision": {
                    "final_schema": final_schema,
                    "confidence_score": 0.7,
                    "decision_rationale": "자동 평가 실패, 기본 스키마 반환",
                    "requires_human_review": True,
                    "warnings": ["Meta-Agent 평가 실패"]
                },
                "sql_script": self._generate_sql(final_schema),
                "thought": "평가 실패, 기본 결정",
                "confidence_score": 0.7
            }

            return self._handle_error(
                error=e,
                fallback_message="JSON 파싱 실패",
                fallback_result=fallback_result
            )

        except Exception as e:
            self.log_execution_failed(e)
            raise

    def _generate_sql(self, schema: Dict) -> str:
        """
        최종 SQL DDL 스크립트 생성

        Args:
            schema: 최종 스키마

        Returns:
            SQL DDL 문자열
        """
        sql_parts = []

        # CREATE TABLE 문들
        tables = schema.get('tables', [])
        for table in tables:
            table_name = table.get('name', 'unknown')
            columns = table.get('columns', [])

            col_defs = []
            for col in columns:
                col_name = col.get('name', '')
                col_type = col.get('type', 'TEXT')
                constraints = col.get('constraints', [])

                # PostgreSQL 예약어 충돌 방지: 컬럼명을 큰따옴표로 감싸기
                col_def = f'    "{col_name}" {col_type}'
                if constraints:
                    col_def += " " + " ".join(constraints)

                col_defs.append(col_def)

            # PostgreSQL 예약어 충돌 방지: 테이블명을 큰따옴표로 감싸기
            sql = f'CREATE TABLE "{table_name}" (\n' + ",\n".join(col_defs) + "\n);"
            sql_parts.append(sql)

        # CREATE INDEX 문들
        for table in tables:
            table_name = table.get('name', '')
            indexes = table.get('indexes', [])

            for idx in indexes:
                # indexes가 문자열 리스트일 수도 있고, dict 리스트일 수도 있음
                if isinstance(idx, dict):
                    idx_name = idx.get('name', '')
                    column_name = idx.get('column', '')
                    if idx_name and column_name:
                        sql = f'CREATE INDEX "{idx_name}" ON "{table_name}"("{column_name}");'
                        sql_parts.append(sql)
                elif isinstance(idx, str):
                    # idx_tablename_columnname 형식에서 컬럼명 추출
                    parts = idx.split('_')
                    if len(parts) >= 3:
                        column_name = '_'.join(parts[2:])
                        sql = f'CREATE INDEX "{idx}" ON "{table_name}"("{column_name}");'
                        sql_parts.append(sql)

        # ALTER TABLE (외래키)
        relations = schema.get('relations', [])
        for rel in relations:
            # camelCase와 snake_case 둘 다 지원
            from_table = rel.get('fromTable') or rel.get('from_table', '')
            to_table = rel.get('toTable') or rel.get('to_table', '')
            from_column = rel.get('fromColumn') or rel.get('from_column', '')
            to_column = rel.get('toColumn') or rel.get('to_column', 'id')

            # 필수 필드가 없으면 건너뛰기
            if not from_table or not to_table or not from_column:
                logger.warning(f"Skipping incomplete relation: {rel}")
                continue

            fk_name = f"fk_{from_table}_{from_column}"
            sql = f'''ALTER TABLE "{from_table}"
    ADD CONSTRAINT "{fk_name}"
    FOREIGN KEY ("{from_column}")
    REFERENCES "{to_table}"("{to_column}")
    ON DELETE CASCADE;'''
            sql_parts.append(sql)

        return "\n\n".join(sql_parts)
