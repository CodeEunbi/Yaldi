"""
Mock Data SQL 검증기

생성된 INSERT 문을 실제 PostgreSQL/MySQL DB에서 실행하여 검증합니다.
"""

import asyncio
import uuid
from typing import Tuple, Optional, List, Literal
import asyncpg
import aiomysql
from config.settings import settings
import logging
from models.schema_models import SchemaData, TableSchema, ColumnSchema

logger = logging.getLogger(__name__)


class MockDataSQLValidator:
    """Mock Data INSERT 문 검증기 (PostgreSQL + MySQL 병렬 검증)"""

    async def validate_insert_statements(
        self,
        schema_data: SchemaData,
        insert_statements: str,
        db_type: Literal["postgresql", "mysql"] = "postgresql"
    ) -> Tuple[bool, Optional[str]]:
        """
        INSERT 문을 실제 DB에서 실행하여 검증

        Args:
            schema_data: 스키마 정보 (테이블, 컬럼)
            insert_statements: 검증할 INSERT 문들
            db_type: 데이터베이스 타입 (기본: postgresql)

        Returns:
            (success: bool, error_message: Optional[str])
        """
        logger.info(f"{db_type.upper()} DB 검증 시작...")

        if db_type == "postgresql":
            return await self._validate_postgres(schema_data, insert_statements)
        elif db_type == "mysql":
            return await self._validate_mysql(schema_data, insert_statements)
        else:
            raise ValueError(f"Unsupported database type: {db_type}")

    async def _validate_postgres(
        self,
        schema_data: SchemaData,
        insert_statements: str
    ) -> Tuple[bool, Optional[str]]:
        """PostgreSQL에서 SQL 검증"""
        schema_name = f"temp_mock_{uuid.uuid4().hex[:8]}"
        conn = None

        try:
            # PostgreSQL 연결
            conn = await asyncpg.connect(settings.TEST_POSTGRES_URL)
            logger.info(f"PostgreSQL 연결 성공, 스키마 생성: {schema_name}")

            # 임시 스키마 생성
            await conn.execute(f"CREATE SCHEMA {schema_name}")
            await conn.execute(f"SET search_path TO {schema_name}")

            # 1. DDL 생성 및 실행 (CREATE TABLE)
            ddl_statements = self._generate_ddl_postgres(schema_data)
            for ddl in ddl_statements:
                logger.debug(f"Executing DDL: {ddl}")
                await conn.execute(ddl)

            # 2. INSERT 문 파싱 및 실행
            insert_list = self._parse_insert_statements(insert_statements)
            for idx, insert_stmt in enumerate(insert_list, 1):
                logger.debug(f"Executing INSERT #{idx}: {insert_stmt[:100]}...")
                await conn.execute(insert_stmt)

            logger.info(f"✅ SQL 검증 성공 - {len(insert_list)}개 INSERT 문 실행 완료")
            return True, None

        except Exception as e:
            error_msg = str(e)
            logger.error(f"❌ PostgreSQL 검증 실패: {error_msg}")
            return False, error_msg

        finally:
            if conn:
                try:
                    # 임시 스키마 삭제
                    await conn.execute(f"DROP SCHEMA IF EXISTS {schema_name} CASCADE")
                    logger.info(f"스키마 정리 완료: {schema_name}")
                except Exception as e:
                    logger.warning(f"스키마 정리 실패: {e}")
                finally:
                    await conn.close()

    async def _validate_mysql(
        self,
        schema_data: SchemaData,
        insert_statements: str
    ) -> Tuple[bool, Optional[str]]:
        """MySQL에서 SQL 검증"""
        db_name = f"temp_mock_{uuid.uuid4().hex[:8]}"
        conn = None

        try:
            # MySQL 연결
            conn = await aiomysql.connect(
                host="test-mysql",
                port=3306,
                user="test",
                password="test",
                db="test_validation"
            )
            cursor = await conn.cursor()
            logger.info(f"MySQL 연결 성공, DB 생성: {db_name}")

            # 임시 데이터베이스 생성
            await cursor.execute(f"CREATE DATABASE {db_name}")
            await cursor.execute(f"USE {db_name}")

            # 1. DDL 생성 및 실행 (CREATE TABLE)
            ddl_statements = self._generate_ddl_mysql(schema_data)
            for ddl in ddl_statements:
                logger.debug(f"Executing DDL: {ddl}")
                await cursor.execute(ddl)

            # 2. INSERT 문 파싱 및 실행
            insert_list = self._parse_insert_statements(insert_statements)
            for idx, insert_stmt in enumerate(insert_list, 1):
                logger.debug(f"Executing INSERT #{idx}: {insert_stmt[:100]}...")
                await cursor.execute(insert_stmt)

            await conn.commit()
            logger.info(f"✅ SQL 검증 성공 - {len(insert_list)}개 INSERT 문 실행 완료")
            return True, None

        except Exception as e:
            error_msg = str(e)
            logger.error(f"❌ MySQL 검증 실패: {error_msg}")
            return False, error_msg

        finally:
            if conn:
                try:
                    cursor = await conn.cursor()
                    # 임시 데이터베이스 삭제
                    await cursor.execute(f"DROP DATABASE IF EXISTS {db_name}")
                    logger.info(f"DB 정리 완료: {db_name}")
                except Exception as e:
                    logger.warning(f"DB 정리 실패: {e}")
                finally:
                    conn.close()

    def _generate_ddl_postgres(self, schema_data: SchemaData) -> List[str]:
        """PostgreSQL용 CREATE TABLE DDL 생성 (Spring schemaData 형식)"""
        ddl_statements = []

        for table in schema_data.tables:
            columns_def = []

            for column in table.columns:
                # 컬럼 타입 (dataType + dataDetail)
                col_type = column.dataType

                # VARCHAR, CHAR 등 크기가 있는 경우
                if column.dataDetail and column.dataType in ["VARCHAR", "CHAR"]:
                    col_type = f"{column.dataType}({column.dataDetail[0]})"
                elif column.dataType == "ENUM" and column.dataDetail:
                    # ENUM은 PostgreSQL에서 TEXT로 처리
                    col_type = "TEXT"

                # AUTO_INCREMENT → SERIAL/BIGSERIAL
                if column.isIncremental:
                    if "BIGINT" in column.dataType.upper():
                        col_type = "BIGSERIAL"
                    else:
                        col_type = "SERIAL"

                col_def = f"{column.physicalName} {col_type}"

                # 제약조건 (Spring 형식)
                if not column.isNullable and not column.isIncremental:
                    col_def += " NOT NULL"
                if column.isUnique:
                    col_def += " UNIQUE"
                if column.isPrimaryKey and not column.isIncremental:
                    col_def += " PRIMARY KEY"

                columns_def.append(col_def)

            ddl = f"CREATE TABLE {table.physicalName} (\n  " + ",\n  ".join(columns_def) + "\n);"
            ddl_statements.append(ddl)

        return ddl_statements

    def _generate_ddl_mysql(self, schema_data: SchemaData) -> List[str]:
        """MySQL용 CREATE TABLE DDL 생성 (Spring schemaData 형식)"""
        ddl_statements = []

        for table in schema_data.tables:
            columns_def = []

            for column in table.columns:
                # 컬럼 타입 (dataType + dataDetail)
                col_type = column.dataType

                # VARCHAR, CHAR 등 크기가 있는 경우
                if column.dataDetail:
                    if column.dataType in ["VARCHAR", "CHAR"]:
                        col_type = f"{column.dataType}({column.dataDetail[0]})"
                    elif column.dataType == "ENUM":
                        # ENUM 타입: dataDetail에 허용값 목록
                        enum_values = ", ".join(f"'{v}'" for v in column.dataDetail)
                        col_type = f"ENUM({enum_values})"

                col_def = f"{column.physicalName} {col_type}"

                # 제약조건 (Spring 형식)
                if column.isIncremental:
                    col_def += " AUTO_INCREMENT"
                if not column.isNullable:
                    col_def += " NOT NULL"
                if column.isUnique:
                    col_def += " UNIQUE"
                if column.isPrimaryKey:
                    col_def += " PRIMARY KEY"

                columns_def.append(col_def)

            ddl = f"CREATE TABLE {table.physicalName} (\n  " + ",\n  ".join(columns_def) + "\n);"
            ddl_statements.append(ddl)

        return ddl_statements

    def _parse_insert_statements(self, insert_statements: str) -> List[str]:
        """
        INSERT 문 문자열을 개별 문장으로 파싱

        Args:
            insert_statements: INSERT 문들 (세미콜론으로 구분)

        Returns:
            개별 INSERT 문 리스트
        """
        # 세미콜론으로 분리
        statements = insert_statements.split(';')

        # 빈 문자열 제거 및 정리
        result = []
        for stmt in statements:
            stmt = stmt.strip()
            if stmt and stmt.upper().startswith('INSERT'):
                result.append(stmt)

        return result


# 싱글톤 인스턴스
mock_data_sql_validator = MockDataSQLValidator()
