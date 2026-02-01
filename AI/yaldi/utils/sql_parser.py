import sqlparse
from sqlparse.sql import IdentifierList, Identifier, Function
from sqlparse.tokens import Keyword, DML
from models.responses.erd_responses import Schema, TableSchema, ColumnSchema
from typing import List
import logging
import re

logger = logging.getLogger(__name__)


class SQLParser:
    """SQL을 파싱하여 JSON 스키마로 변환"""

    def parse_sql_to_schema(self, sql_content: str) -> Schema:
        """
        SQL을 파싱하여 Schema 객체로 변환

        Args:
            sql_content: CREATE TABLE 문들이 포함된 SQL

        Returns:
            Schema 객체
        """
        tables = []
        statements = sqlparse.split(sql_content)

        for statement in statements:
            if statement.strip():
                parsed = sqlparse.parse(statement)[0]
                if self._is_create_table(parsed):
                    table = self._parse_create_table(statement)
                    if table:
                        tables.append(table)

        return Schema(tables=tables)

    def _is_create_table(self, parsed) -> bool:
        """CREATE TABLE 문인지 확인"""
        statement_upper = str(parsed).strip().upper()
        return statement_upper.startswith('CREATE TABLE')

    def _parse_create_table(self, statement: str) -> TableSchema:
        """CREATE TABLE 문을 파싱하여 TableSchema 생성"""
        try:
            # 테이블명 추출
            table_name_match = re.search(r'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(\w+)', statement, re.IGNORECASE)
            if not table_name_match:
                return None

            table_name = table_name_match.group(1)

            # 컬럼 정의 추출 (괄호 안의 내용)
            columns_match = re.search(r'\((.*)\)', statement, re.DOTALL)
            if not columns_match:
                return None

            columns_str = columns_match.group(1)
            columns = self._parse_columns(columns_str)

            return TableSchema(name=table_name, columns=columns)

        except Exception as e:
            logger.error(f"Failed to parse CREATE TABLE: {e}")
            return None

    def _parse_columns(self, columns_str: str) -> List[ColumnSchema]:
        """컬럼 정의 문자열을 파싱하여 ColumnSchema 리스트 생성"""
        columns = []

        # 컬럼 정의를 쉼표로 분리 - 괄호 안의 쉼표는 제외하고
        column_defs = self._split_column_definitions(columns_str)

        for col_def in column_defs:
            col_def = col_def.strip()
            if not col_def or col_def.upper().startswith(('PRIMARY KEY', 'FOREIGN KEY', 'UNIQUE', 'CHECK', 'CONSTRAINT')):
                continue

            # 컬럼명과 타입 추출
            parts = col_def.split()
            if len(parts) < 2:
                continue

            col_name = parts[0].strip('`"')
            col_type = parts[1]

            # 제약조건 추출
            constraints = []
            remaining = ' '.join(parts[2:]).upper()

            if 'PRIMARY KEY' in remaining:
                constraints.append('PRIMARY KEY')
            if 'NOT NULL' in remaining:
                constraints.append('NOT NULL')
            if 'UNIQUE' in remaining:
                constraints.append('UNIQUE')
            if 'AUTO_INCREMENT' in remaining or 'AUTOINCREMENT' in remaining:
                constraints.append('AUTO_INCREMENT')

            columns.append(ColumnSchema(
                name=col_name,
                type=col_type,
                constraints=constraints if constraints else None
            ))

        return columns

    def _split_column_definitions(self, columns_str: str) -> List[str]:
        """컬럼 정의를 쉼표로 분리 (괄호 depth를 고려)"""
        result = []
        current = []
        depth = 0

        for char in columns_str:
            if char == '(':
                depth += 1
                current.append(char)
            elif char == ')':
                depth -= 1
                current.append(char)
            elif char == ',' and depth == 0:
                result.append(''.join(current))
                current = []
            else:
                current.append(char)

        if current:
            result.append(''.join(current))

        return result


sql_parser = SQLParser()
