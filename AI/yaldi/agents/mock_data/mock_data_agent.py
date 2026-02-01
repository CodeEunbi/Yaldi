"""
Mock Data 생성 전용 AI Agent

스키마 정보를 기반으로 INSERT 문을 생성하고, SQL 검증기로 실행 테스트합니다.
에러 발생 시 자동으로 수정하는 Self-correction 기능을 포함합니다.
"""

from agents.base_agent import BaseAgent
from typing import Dict, Any
from utils.prompt_loader import prompt_loader
from models.requests.mockdata_requests import MockDataCreateRequest
from models.schema_models import TableSchema
from .sql_validator import mock_data_sql_validator
from config.constants import AgentConstants
import logging

logger = logging.getLogger(__name__)


class MockDataAgent(BaseAgent):
    """Mock Data 생성 및 검증 Agent"""

    def __init__(self, max_retries: int = None):
        """
        Args:
            max_retries: 최대 재시도 횟수 (기본값: AgentConstants.MAX_MOCK_DATA_RETRIES)
        """
        # BaseAgent 초기화 (temperature=0.7: 창의적인 데이터 생성)
        super().__init__(agent_name="MockDataAgent", temperature=0.7)

        self.validator = mock_data_sql_validator
        self.max_retries = max_retries if max_retries is not None else AgentConstants.MAX_MOCK_DATA_RETRIES

    async def execute(self, **kwargs) -> Dict[str, Any]:
        """
        BaseAgent의 추상 메서드 구현
        generate_mock_data 메서드로 위임
        """
        request = kwargs.get("request")
        if not request:
            raise ValueError("MockDataCreateRequest is required")

        result = await self.generate_mock_data(request)
        return {"sql_statements": result}

    async def generate_mock_data(self, request: MockDataCreateRequest) -> str:
        """
        Mock Data INSERT 문 생성 (자동 검증 및 수정 포함)

        Args:
            request: Mock Data 생성 요청

        Returns:
            str: 검증 완료된 INSERT SQL 문들

        Raises:
            Exception: 최대 재시도 횟수 초과 시
        """
        self.log_execution_start(
            table_count=len(request.schema_data.tables),
            row_count=request.row_count
        )

        # 1. LLM으로 DB 타입 자동 감지
        schema_description = self._format_schema_description(request.schema_data.tables)
        db_type = await self._detect_db_type(schema_description)
        self.logger.info(f"감지된 DB 타입: {db_type}")

        sql_statements = None

        # 재시도 루프
        for attempt in range(1, self.max_retries + 1):
            try:
                self.logger.info(f"시도 {attempt}/{self.max_retries}")

                # 1. LLM으로 INSERT 문 생성
                if attempt == 1:
                    # 첫 시도: 일반 생성
                    sql_statements = await self._generate_initial_sql(
                        schema_description,
                        request.row_count
                    )
                else:
                    # 재시도: 에러 수정
                    sql_statements = await self._fix_sql_error(
                        schema_description,
                        request.row_count,
                        sql_statements,
                        error_message
                    )

                self.logger.info(f"SQL 생성 완료 - 길이: {len(sql_statements)} bytes")

                # 2. SQL 검증 (감지된 DB 타입으로 검증)
                is_valid, error_message = await self.validator.validate_insert_statements(
                    request.schema_data,
                    sql_statements,
                    db_type=db_type
                )

                if is_valid:
                    self.logger.info(f"✅ Mock Data 생성 성공 (시도 {attempt}회)")
                    return sql_statements

                # 검증 실패
                self.logger.warning(f"❌ SQL 검증 실패 (시도 {attempt}/{self.max_retries})")
                self.logger.warning(f"오류: {error_message}")

                if attempt == self.max_retries:
                    # 최대 재시도 초과
                    error = Exception(
                        f"Mock Data 생성 실패: {self.max_retries}회 시도 후에도 유효한 SQL을 생성하지 못했습니다. "
                        f"마지막 오류: {error_message}"
                    )
                    self.log_execution_failed(error)
                    raise error

            except Exception as e:
                if attempt == self.max_retries:
                    self.log_execution_failed(e)
                    raise
                self.logger.warning(f"시도 {attempt} 실패, 재시도 중...")

        # 여기 도달하면 안 됨
        error = Exception("예상치 못한 오류: 재시도 루프 종료")
        self.log_execution_failed(error)
        raise error

    async def _generate_initial_sql(
        self,
        schema_description: str,
        row_count: int
    ) -> str:
        """
        최초 INSERT 문 생성

        Args:
            schema_description: 포맷팅된 스키마 설명
            row_count: 생성할 행 수

        Returns:
            생성된 INSERT SQL 문들
        """
        from langchain_core.messages import SystemMessage, HumanMessage

        # 프롬프트 로드
        user_prompt = prompt_loader.load(
            "mock_data_generation",
            schema_description=schema_description,
            row_count=row_count
        )

        # LLM 호출
        messages = [
            SystemMessage(content="당신은 데이터베이스 Mock Data를 생성하는 전문가입니다. 주어진 스키마를 분석하고 현실적인 INSERT 문을 생성합니다."),
            HumanMessage(content=user_prompt)
        ]

        response = await self.llm.ainvoke(messages)

        return self._extract_sql_statements(response.content)

    async def _fix_sql_error(
        self,
        schema_description: str,
        row_count: int,
        previous_sql: str,
        error_message: str
    ) -> str:
        """
        에러가 발생한 SQL을 수정

        Args:
            schema_description: 포맷팅된 스키마 설명
            row_count: 생성할 행 수
            previous_sql: 이전에 생성한 SQL
            error_message: 발생한 오류 메시지

        Returns:
            수정된 INSERT SQL 문들
        """
        from langchain_core.messages import SystemMessage, HumanMessage
        from langchain_openai import ChatOpenAI

        self.logger.info("LLM에 SQL 오류 수정 요청 중...")

        # 에러 수정 프롬프트 로드
        user_prompt = prompt_loader.load(
            "mock_data_error_fix",
            schema_description=schema_description,
            row_count=row_count,
            previous_sql=previous_sql,
            error_message=error_message
        )

        # 에러 수정용 LLM (temperature 낮춤)
        error_fix_llm = ChatOpenAI(
            base_url=self.llm.base_url,
            api_key=self.llm.openai_api_key,
            model_name=self.llm.model_name,
            temperature=0.5,  # 에러 수정은 좀 더 보수적으로
            max_tokens=4000
        )

        # LLM 호출
        messages = [
            SystemMessage(content="당신은 데이터베이스 오류를 분석하고 수정하는 전문가입니다. 주어진 오류를 분석하고 올바른 SQL을 생성합니다."),
            HumanMessage(content=user_prompt)
        ]

        response = await error_fix_llm.ainvoke(messages)

        return self._extract_sql_statements(response.content)

    def _format_schema_description(self, tables: list[TableSchema]) -> str:
        """
        스키마 정보를 LLM이 이해하기 쉬운 형태로 포맷팅 (Spring schemaData 형식)

        Args:
            tables: 테이블 스키마 목록

        Returns:
            str: 포맷팅된 스키마 설명
        """
        schema_lines = []

        for table in tables:
            # Spring 형식: physicalName, logicalName
            schema_lines.append(f"\n테이블: {table.physicalName} ({table.logicalName})")
            schema_lines.append("컬럼:")

            for column in table.columns:
                # Spring 형식에서 제약조건 추출
                constraints = []
                if column.isPrimaryKey:
                    constraints.append("PRIMARY KEY")
                if column.isUnique:
                    constraints.append("UNIQUE")
                if not column.isNullable:
                    constraints.append("NOT NULL")
                if column.isForeignKey:
                    constraints.append("FOREIGN KEY")
                if column.isIncremental:
                    constraints.append("AUTO_INCREMENT")

                constraints_str = ", ".join(constraints) if constraints else "없음"

                # dataType + dataDetail (예: VARCHAR(100), ENUM 값)
                data_type_full = column.dataType
                if column.dataDetail:
                    if column.dataType == "ENUM":
                        # ENUM 타입: dataDetail에 허용값 목록
                        enum_str = ", ".join(f"'{v}'" for v in column.dataDetail)
                        data_type_full += f"({enum_str})"
                    elif column.dataType in ["VARCHAR", "CHAR"]:
                        # VARCHAR, CHAR: dataDetail[0]이 크기
                        data_type_full += f"({column.dataDetail[0]})"

                column_desc = f"  - {column.physicalName} ({column.logicalName}): {data_type_full} (제약조건: {constraints_str})"

                # 기본값이 있으면 추가
                if column.defaultValue:
                    column_desc += f" [기본값: {column.defaultValue}]"

                schema_lines.append(column_desc)

        return "\n".join(schema_lines)

    def _extract_sql_statements(self, llm_response: str) -> str:
        """
        LLM 응답에서 INSERT 문만 추출 (설명이나 주석 제거)

        Args:
            llm_response: LLM 응답 전체 텍스트

        Returns:
            INSERT 문만 포함된 문자열
        """
        sql_content = llm_response.strip()

        # SQL 문만 추출
        sql_lines = []
        for line in sql_content.split("\n"):
            line = line.strip()
            if line and (line.upper().startswith("INSERT") or (sql_lines and not line.startswith("#") and not line.startswith("--"))):
                sql_lines.append(line)

        return "\n".join(sql_lines)

    async def _detect_db_type(self, schema_description: str) -> str:
        """
        LLM을 사용하여 스키마에서 DB 타입 자동 감지

        Args:
            schema_description: 포맷팅된 스키마 설명

        Returns:
            "postgresql" 또는 "mysql"
        """
        from langchain_core.messages import SystemMessage, HumanMessage
        from langchain_openai import ChatOpenAI

        self.logger.info("LLM으로 DB 타입 자동 감지 중...")

        # 프롬프트 로드
        user_prompt = prompt_loader.load(
            "detect_db_type",
            schema_description=schema_description
        )

        # DB 타입 감지용 LLM (temperature 0)
        detection_llm = ChatOpenAI(
            base_url=self.llm.base_url,
            api_key=self.llm.openai_api_key,
            model_name=self.llm.model_name,
            temperature=0.0,  # 결정론적 응답
            max_tokens=50,
            model_kwargs={"response_format": {"type": "json_object"}}
        )

        # LLM 호출 (JSON 응답)
        try:
            messages = [
                SystemMessage(content="당신은 데이터베이스 스키마 분석 전문가입니다. 스키마를 보고 PostgreSQL인지 MySQL인지 정확히 판단합니다. JSON 형식으로 응답하세요."),
                HumanMessage(content=user_prompt)
            ]

            response = await detection_llm.ainvoke(messages)
            result = self._parse_json_response(response.content)

            db_type = result.get("dbType", "postgresql").lower()

            # 유효성 검증
            if db_type not in ["postgresql", "mysql"]:
                self.logger.warning(f"알 수 없는 DB 타입: {db_type}, 기본값(postgresql) 사용")
                db_type = "postgresql"

            return db_type

        except Exception as e:
            self.logger.error(f"DB 타입 감지 실패: {e}, 기본값(postgresql) 사용")
            return "postgresql"



# 인스턴스 생성 (필요시 여러 개 생성 가능, Agent는 상태를 유지하지 않음)
# 일반적으로 하나의 인스턴스만 사용
mock_data_agent = MockDataAgent()
