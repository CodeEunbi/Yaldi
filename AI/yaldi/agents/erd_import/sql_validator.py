import asyncio
import uuid
from typing import Literal, Optional, Tuple
import asyncpg
import aiomysql
from config.settings import settings
import logging
import sqlparse

logger = logging.getLogger(__name__)


class SQLValidator:
    """SQL 빌드 검증 에이전트 tool"""

    async def validate_sql(
        self,
        sql_content: str,
        db_type: Literal["postgresql", "mysql"]
    ) -> Tuple[bool, Optional[str]]:
        """
        SQL을 테스트 DB에서 실행하여 검증

        Args:
            sql_content: 검증할 SQL
            db_type: 데이터베이스 타입

        Returns:
            (success: bool, error_message: Optional[str])
        """
        if db_type == "postgresql":
            return await self._validate_postgres(sql_content)
        elif db_type == "mysql":
            return await self._validate_mysql(sql_content)
        else:
            raise ValueError(f"Unsupported database type: {db_type}")

    async def _validate_postgres(self, sql_content: str) -> Tuple[bool, Optional[str]]:
        """PostgreSQL에서 SQL 검증"""
        schema_name = f"temp_validation_{uuid.uuid4().hex[:8]}"
        conn = None

        try:
            # PostgreSQL 연결
            conn = await asyncpg.connect(settings.TEST_POSTGRES_URL)
            logger.info(f"Connected to PostgreSQL, creating schema: {schema_name}")

            # 임시 스키마 생성
            await conn.execute(f"CREATE SCHEMA {schema_name}")
            await conn.execute(f"SET search_path TO {schema_name}")

            # SQL 파싱 및 실행
            statements = sqlparse.split(sql_content)
            for statement in statements:
                if statement.strip():
                    logger.debug(f"Executing: {statement}")
                    await conn.execute(statement)

            logger.info("SQL validation successful")
            return True, None

        except Exception as e:
            error_msg = str(e)
            logger.error(f"PostgreSQL validation error: {error_msg}")
            return False, error_msg

        finally:
            if conn:
                try:
                    # 임시 스키마 삭제
                    await conn.execute(f"DROP SCHEMA IF EXISTS {schema_name} CASCADE")
                    logger.info(f"Cleaned up schema: {schema_name}")
                except Exception as e:
                    logger.warning(f"Failed to cleanup schema: {e}")
                finally:
                    await conn.close()

    async def _validate_mysql(self, sql_content: str) -> Tuple[bool, Optional[str]]:
        """MySQL에서 SQL 검증"""
        db_name = f"temp_validation_{uuid.uuid4().hex[:8]}"
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
            logger.info(f"Connected to MySQL, creating database: {db_name}")

            # 임시 데이터베이스 생성
            await cursor.execute(f"CREATE DATABASE {db_name}")
            await cursor.execute(f"USE {db_name}")

            # SQL 파싱 및 실행
            statements = sqlparse.split(sql_content)
            for statement in statements:
                if statement.strip():
                    logger.debug(f"Executing: {statement}")
                    await cursor.execute(statement)

            await conn.commit()
            logger.info("SQL validation successful")
            return True, None

        except Exception as e:
            error_msg = str(e)
            logger.error(f"MySQL validation error: {error_msg}")
            return False, error_msg

        finally:
            if conn:
                try:
                    cursor = await conn.cursor()
                    # 임시 데이터베이스 삭제
                    await cursor.execute(f"DROP DATABASE IF EXISTS {db_name}")
                    logger.info(f"Cleaned up database: {db_name}")
                except Exception as e:
                    logger.warning(f"Failed to cleanup database: {e}")
                finally:
                    conn.close()


# Singleton instance
sql_validator = SQLValidator()
