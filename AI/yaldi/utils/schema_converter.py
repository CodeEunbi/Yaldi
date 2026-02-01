from models.schema_models import SchemaData, TableSchema, ColumnSchema, RelationSchema
from typing import List, Dict
import logging

logger = logging.getLogger(__name__)


class SchemaToSQLConverter:
    """SchemaData를 SQL CREATE TABLE 문으로 변환"""

    def convert_to_sql(self, schema_data: SchemaData) -> str:
        """
        SchemaData를 SQL DDL로 변환

        Args:
            schema_data: 변환할 스키마 데이터

        Returns:
            SQL CREATE TABLE 문들
        """
        sql_statements = []

        # 1. 테이블명 매핑 (tableKey -> physicalName)
        table_map: Dict[int, str] = {
            table.tableKey: table.physicalName
            for table in schema_data.tables
        }

        # 2. 각 테이블에 대해 CREATE TABLE 문 생성
        for table in schema_data.tables:
            create_table_sql = self._generate_create_table(table)
            sql_statements.append(create_table_sql)

        # 3. 외래키 제약조건 추가 (ALTER TABLE)
        if schema_data.relations:
            for relation in schema_data.relations:
                alter_sql = self._generate_foreign_key(relation, table_map, schema_data.tables)
                if alter_sql:
                    sql_statements.append(alter_sql)

        return "\n\n".join(sql_statements)

    def _generate_create_table(self, table: TableSchema) -> str:
        """단일 테이블의 CREATE TABLE 문 생성"""
        column_definitions = []
        primary_keys = []

        for column in table.columns:
            col_def = self._generate_column_definition(column)
            column_definitions.append(col_def)

            if column.isPrimaryKey:
                primary_keys.append(column.physicalName)

        # PRIMARY KEY 제약조건
        if primary_keys:
            pk_constraint = f"PRIMARY KEY ({', '.join(primary_keys)})"
            column_definitions.append(pk_constraint)

        columns_str = ",\n    ".join(column_definitions)
        return f"CREATE TABLE {table.physicalName} (\n    {columns_str}\n);"

    def _generate_column_definition(self, column: ColumnSchema) -> str:
        """단일 컬럼의 정의 생성"""
        parts = [column.physicalName]

        # 데이터 타입
        data_type = self._format_data_type(column.dataType, column.dataDetail)
        parts.append(data_type)

        # NOT NULL
        if not column.isNullable:
            parts.append("NOT NULL")

        # UNIQUE
        if column.isUnique:
            parts.append("UNIQUE")

        # AUTO_INCREMENT
        if column.isIncremental:
            parts.append("AUTO_INCREMENT")

        # DEFAULT
        if column.defaultValue:
            parts.append(f"DEFAULT {column.defaultValue}")

        return " ".join(parts)

    def _format_data_type(self, data_type: str, data_detail: List[str]) -> str:
        """데이터 타입 포맷팅"""
        data_type_upper = data_type.upper()

        # ENUM 타입
        if data_type_upper == "ENUM" and data_detail:
            enum_values = ", ".join([f"'{val}'" for val in data_detail])
            return f"ENUM({enum_values})"

        # VARCHAR, CHAR 등 크기 지정 타입
        if data_detail and data_type_upper in ["VARCHAR", "CHAR", "DECIMAL", "NUMERIC"]:
            if len(data_detail) == 1:
                return f"{data_type_upper}({data_detail[0]})"
            elif len(data_detail) == 2:
                return f"{data_type_upper}({data_detail[0]}, {data_detail[1]})"

        return data_type_upper

    def _generate_foreign_key(
        self,
        relation: RelationSchema,
        table_map: Dict[int, str],
        tables: List[TableSchema]
    ) -> str:
        """외래키 제약조건 ALTER TABLE 문 생성"""
        try:
            from_table_name = table_map.get(relation.fromTableKey)
            to_table_name = table_map.get(relation.toTableKey)

            if not from_table_name or not to_table_name:
                logger.warning(f"테이블을 찾을 수 없음: from={relation.fromTableKey}, to={relation.toTableKey}")
                return ""

            # FROM 테이블에서 FK 컬럼 찾기
            from_table = next((t for t in tables if t.tableKey == relation.fromTableKey), None)
            if not from_table:
                return ""

            fk_columns = [col.physicalName for col in from_table.columns if col.isForeignKey]
            if not fk_columns:
                logger.warning(f"테이블 {from_table_name}에 FK 컬럼이 없음")
                return ""

            # TO 테이블의 PK 컬럼 찾기
            to_table = next((t for t in tables if t.tableKey == relation.toTableKey), None)
            if not to_table:
                return ""

            pk_columns = [col.physicalName for col in to_table.columns if col.isPrimaryKey]
            if not pk_columns:
                logger.warning(f"테이블 {to_table_name}에 PK 컬럼이 없음")
                return ""

            # 제약조건명
            constraint_name = relation.constraintName or f"fk_{from_table_name}_{to_table_name}"

            # ALTER TABLE 문
            alter_parts = [
                f"ALTER TABLE {from_table_name}",
                f"ADD CONSTRAINT {constraint_name}",
                f"FOREIGN KEY ({', '.join(fk_columns)})",
                f"REFERENCES {to_table_name} ({', '.join(pk_columns)})"
            ]

            # ON DELETE / ON UPDATE
            if relation.onDeleteAction:
                alter_parts.append(f"ON DELETE {relation.onDeleteAction}")
            if relation.onUpdateAction:
                alter_parts.append(f"ON UPDATE {relation.onUpdateAction}")

            return " ".join(alter_parts) + ";"

        except Exception as e:
            logger.error(f"외래키 생성 중 오류: {e}")
            return ""


schema_converter = SchemaToSQLConverter()
