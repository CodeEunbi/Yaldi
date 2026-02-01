"""
Validator Agent

스키마 검증 전문 Agent (Agentic Tool Use)
"""
from langchain_openai import ChatOpenAI
from langchain.agents import Tool, AgentExecutor, create_react_agent
from langchain_core.prompts import PromptTemplate
from typing import Dict, List
from config.settings import settings
import asyncpg
import logging
import json

logger = logging.getLogger(__name__)


class ValidatorAgent:
    """
    스키마 검증 전문 Agent

    역할:
    - 순환 참조 검증
    - 정규화 검증
    - 제약조건 검증
    - **실제 DB 테스트 수행** (Agentic Tool Use)
    """

    def __init__(self):
        self.llm = ChatOpenAI(
            base_url=settings.GMS_BASE_URL,
            api_key=settings.GMS_API_KEY,
            model=settings.OPENAI_MODEL,
            temperature=0
        )

        # Agent Tools 정의
        self.tools = [
            Tool(
                name="check_circular_references",
                func=self._check_circular_references,
                description="외래키 순환 참조를 검사합니다. 입력: JSON 형식의 스키마"
            ),
            Tool(
                name="test_schema_creation",
                func=self._test_schema_creation,
                description="실제 Test PostgreSQL DB에 스키마를 생성해봅니다. 입력: JSON 형식의 스키마"
            ),
            Tool(
                name="check_performance_issues",
                func=self._check_performance_issues,
                description="성능 이슈(인덱스 누락 등)를 검사합니다. 입력: JSON 형식의 스키마"
            )
        ]

    async def validate(self, schema: Dict) -> Dict:
        """
        전체 검증 실행

        Args:
            schema: 검증할 스키마

        Returns:
            {
                "agent": "Validator",
                "issues": [...],
                "is_valid": True/False,
                "thought": "...",
                "raw_checks": {...}
            }
        """
        try:
            logger.info("[Validator] Starting comprehensive schema validation...")

            # 각 검증 도구 실행
            issues = []

            # 1. 순환 참조 체크
            logger.debug("[Validator] Checking circular references...")
            circular_result = self._check_circular_references(json.dumps(schema))
            logger.debug(f"[Validator] Circular check result: {circular_result}")
            if "❌" in circular_result:
                issues.append(circular_result)

            # 2. 실제 DB 생성 테스트
            logger.debug("[Validator] Testing actual DB schema creation...")
            db_test_result = await self._test_schema_creation_async(schema)
            logger.debug(f"[Validator] DB test result: {db_test_result}")
            if "❌" in db_test_result:
                issues.append(db_test_result)

            # 3. 성능 이슈 체크
            logger.debug("[Validator] Checking performance issues...")
            perf_result = self._check_performance_issues(json.dumps(schema))
            logger.debug(f"[Validator] Performance check result: {perf_result}")
            if "⚠️" in perf_result or "❌" in perf_result:
                issues.append(perf_result)

            is_valid = len(issues) == 0
            if is_valid:
                logger.info("[Validator] PASSED - No issues found")
            else:
                logger.warning(f"[Validator] FAILED - {len(issues)} issues found: {issues}")
            
            return {
                "agent": "Validator",
                "issues": issues,
                "is_valid": is_valid,
                "thought": f"검증 완료. {len(issues)}개의 문제 발견",
                "raw_checks": {
                    "circular_check": circular_result,
                    "db_test": db_test_result,
                    "performance": perf_result
                }
            }

        except Exception as e:
            logger.error(f"[Validator] FAILED with exception: {type(e).__name__}: {str(e)}", exc_info=True)
            return {
                "agent": "Validator",
                "issues": [f"검증 중 오류 발생: {str(e)}"],
                "is_valid": False,
                "thought": "검증 실패",
                "raw_checks": {}
            }

    def _check_circular_references(self, schema_json: str) -> str:
        """순환 참조 검사"""
        try:
            schema = json.loads(schema_json)

            # 관계 그래프 구축
            graph = {}
            relations = schema.get('relations', [])

            for rel in relations:
                # camelCase와 snake_case 둘 다 지원
                from_table = rel.get('fromTable') or rel.get('from_table', '')
                to_table = rel.get('toTable') or rel.get('to_table', '')

                # 빈 테이블명 건너뛰기
                if not from_table or not to_table:
                    logger.warning(f"Skipping relation with empty table name: {rel}")
                    continue


            # DFS로 순환 검사
            def has_cycle(node, visited, rec_stack, path):
                visited.add(node)
                rec_stack.add(node)
                path.append(node)

                for neighbor in graph.get(node, []):
                    if neighbor not in visited:
                        if has_cycle(neighbor, visited, rec_stack, path):
                            return True
                    elif neighbor in rec_stack:
                        # 순환 발견
                        cycle_start = path.index(neighbor)
                        cycle = path[cycle_start:] + [neighbor]
                        logger.warning(f"Circular reference found: {' -> '.join(cycle)}")
                        return True

                path.pop()
                rec_stack.remove(node)
                return False

            # 모든 노드에서 검사
            visited = set()
            for node in graph:
                if node not in visited:
                    if has_cycle(node, visited, set(), []):
                        return f"❌ 순환 참조 발견"

            return "✅ 순환 참조 없음"

        except Exception as e:
            logger.error(f"Circular check error: {e}")
            return f"⚠️ 순환 참조 체크 실패: {str(e)}"

    async def _test_schema_creation_async(self, schema: Dict) -> str:
        """실제 DB에 스키마 생성 테스트 (비동기)"""
        conn = None
        try:
            logger.debug("[Validator] Connecting to test PostgreSQL database...")
            conn = await asyncpg.connect(settings.TEST_POSTGRES_URL)

            # 기존 스키마 삭제
            await conn.execute("DROP SCHEMA IF EXISTS test_validation CASCADE")
            await conn.execute("CREATE SCHEMA test_validation")
            await conn.execute("SET search_path TO test_validation")

            # 테이블 생성
            tables = schema.get('tables', [])
            for table in tables:
                ddl = self._generate_create_table_ddl(table)
                logger.debug(f"Executing DDL: {ddl}")
                await conn.execute(ddl)

            # 외래키 추가
            relations = schema.get('relations', [])
            for rel in relations:
                fk_ddl = self._generate_foreign_key_ddl(rel)
                if fk_ddl:
                    logger.debug(f"Executing FK: {fk_ddl}")
                    await conn.execute(fk_ddl)

            logger.info("✅ Test DB schema creation SUCCESS")
            return "✅ 테스트 DB에 스키마 생성 성공"

        except Exception as e:
            error_msg = str(e)
            logger.error(f"DB creation failed: {error_msg}")
            return f"❌ DB 생성 실패: {error_msg}"

        finally:
            if conn:
                await conn.close()

    def _test_schema_creation(self, schema_json: str) -> str:
        """동기 래퍼 (Tool에서 사용)"""
        import asyncio
        schema = json.loads(schema_json)
        loop = asyncio.get_event_loop()
        return loop.run_until_complete(self._test_schema_creation_async(schema))

    def _check_performance_issues(self, schema_json: str) -> str:
        """성능 이슈 검사"""
        try:
            schema = json.loads(schema_json)
            issues = []

            # FK에 인덱스 있는지 확인
            relations = schema.get('relations', [])
            tables = {t['name']: t for t in schema.get('tables', [])}

            for rel in relations:
                from_table_name = rel.get('fromTable', '')
                from_column = rel.get('fromColumn', '')

                table = tables.get(from_table_name)
                if table:
                    indexes = table.get('indexes', [])
                    # from_column이 인덱스에 포함되어 있는지 확인
                    has_index = any(from_column in idx for idx in indexes)

                    if not has_index:
                        issues.append(f"{from_table_name}.{from_column} (FK에 인덱스 없음)")

            if issues:
                return f"⚠️ 성능 이슈: {', '.join(issues)}"

            return "✅ 성능 이슈 없음"

        except Exception as e:
            logger.error(f"Performance check error: {e}")
            return f"⚠️ 성능 체크 실패: {str(e)}"

    def _generate_create_table_ddl(self, table: Dict) -> str:
        """CREATE TABLE DDL 생성"""
        table_name = table.get('name', 'unknown')
        columns = table.get('columns', [])

        col_defs = []
        for col in columns:
            col_name = col.get('name', '')
            col_type = col.get('type', 'TEXT')
            constraints = col.get('constraints', [])

            col_def = f'"{col_name}" {col_type}'
            if constraints:
                col_def += " " + " ".join(constraints)

            col_defs.append(col_def)

        ddl = f'CREATE TABLE "{table_name}" ({", ".join(col_defs)})'
        return ddl

    def _generate_foreign_key_ddl(self, relation: Dict) -> str:
        """외래키 DDL 생성"""
        try:
            # camelCase와 snake_case 둘 다 지원
            from_table = relation.get('fromTable') or relation.get('from_table', '')
            to_table = relation.get('toTable') or relation.get('to_table', '')
            from_column = relation.get('fromColumn') or relation.get('from_column', '')
            to_column = relation.get('toColumn') or relation.get('to_column', 'id')

            # 필수 필드가 없으면 건너뛰기
            if not from_table or not to_table or not from_column:
                logger.warning(f"Skipping incomplete relation: {relation}")
                return ""

            fk_name = f"fk_{from_table}_{from_column}"

            # PostgreSQL 예약어 충돌 방지: 모든 식별자를 큰따옴표로 감싸기
            ddl = f'ALTER TABLE "{from_table}" ADD CONSTRAINT "{fk_name}" FOREIGN KEY ("{from_column}") REFERENCES "{to_table}"("{to_column}") ON DELETE CASCADE'
            return ddl

        except Exception as e:
            logger.error(f"FK DDL generation failed: {e}")
            return ""
