"""
Neo4j 기반 Graph RAG

엔티티 관계 패턴을 그래프로 저장하고 검색
"""
from neo4j import AsyncGraphDatabase
from typing import List, Dict, Optional
from config.settings import settings
from core.singleton import SingletonMeta
import logging

logger = logging.getLogger(__name__)


class GraphRAG(metaclass=SingletonMeta):
    """
    Neo4j 기반 Graph RAG

    지식 그래프 구조:
    (Project)-[:HAS_ENTITY]->(Entity)-[:RELATES_TO]->(Entity)
    (Entity)-[:HAS_ATTRIBUTE]->(Attribute)

    Singleton 패턴 적용: Neo4j 연결을 하나만 유지
    """

    def __init__(self):
        # Singleton이므로 중복 초기화 방지
        if hasattr(self, '_initialized'):
            return
        self._initialized = True

        self.driver = AsyncGraphDatabase.driver(
            settings.NEO4J_URI,
            auth=(settings.NEO4J_USER, settings.NEO4J_PASSWORD)
        )
        logger.info(f"GraphRAG singleton instance created with URI: {settings.NEO4J_URI}")

    async def close(self):
        """연결 종료"""
        await self.driver.close()

    async def index_project_schema(
        self,
        version_key: int,
        version_name: str,
        version_description: str,
        project_name: str,
        project_description: str,
        schema_data: Dict,
        is_public: bool = True,
        design_verification_status: str = "SUCCESS"
    ):
        """
        프로젝트 스키마를 그래프로 저장

        Args:
            version_key: 버전 키
            version_name: 버전 이름
            version_description: 버전 설명
            project_name: 프로젝트 이름
            project_description: 프로젝트 설명
            schema_data: 스키마 데이터 (tables, relations 포함)
            is_public: 공개 여부
            design_verification_status: 검증 상태
        """
        async with self.driver.session() as session:
            try:
                # 프로젝트 노드 생성 (빈 문자열 처리)
                params = {
                    "version_key": version_key,
                    "project_name": project_name,
                    "version_name": version_name,
                    "is_public": is_public,
                    "status": design_verification_status
                }

                # 빈 문자열이 아닌 경우에만 추가
                if project_description and project_description.strip():
                    params["project_description"] = project_description
                if version_description and version_description.strip():
                    params["version_description"] = version_description

                # 동적 쿼리 생성
                set_clauses = [
                    "p.project_name = $project_name",
                    "p.version_name = $version_name",
                    "p.is_public = $is_public",
                    "p.verification_status = $status",
                    "p.indexed_at = datetime()"
                ]

                if "project_description" in params:
                    set_clauses.append("p.project_description = $project_description")
                if "version_description" in params:
                    set_clauses.append("p.version_description = $version_description")

                query = f"""
                    MERGE (p:Project {{version_key: $version_key}})
                    SET {", ".join(set_clauses)}
                """

                await session.run(query, **params)

                # 엔티티 생성
                tables = schema_data.get('tables', [])
                for table in tables:
                    table_name = table.get('name', '')
                    columns = table.get('columns', [])

                    await session.run("""
                        MATCH (p:Project {version_key: $version_key})
                        MERGE (e:Entity {name: $entity_name, version_key: $version_key})
                        SET e.columns = $columns
                        MERGE (p)-[:HAS_ENTITY]->(e)
                    """, version_key=version_key, entity_name=table_name,
                         columns=[col.get('name', '') for col in columns])

                # 관계 생성
                relations = schema_data.get('relations', [])
                for rel in relations:
                    from_table = rel.get('fromTable', '')
                    to_table = rel.get('toTable', '')
                    rel_type = rel.get('type', 'UNKNOWN')

                    await session.run("""
                        MATCH (from:Entity {name: $from_name, version_key: $version_key})
                        MATCH (to:Entity {name: $to_name, version_key: $version_key})
                        MERGE (from)-[r:RELATES_TO {type: $rel_type}]->(to)
                    """, version_key=version_key, from_name=from_table,
                         to_name=to_table, rel_type=rel_type)

                logger.info(f"Indexed project to Neo4j: version_key={version_key}")

            except Exception as e:
                logger.error(f"Failed to index to Neo4j: {e}", exc_info=True)
                raise

    async def search_similar_patterns(
        self,
        keywords: List[str],
        top_k: int = 5
    ) -> List[Dict]:
        """
        키워드와 관련된 그래프 패턴 검색

        Args:
            keywords: 검색 키워드 리스트
            top_k: 반환할 최대 결과 수

        Returns:
            유사 프로젝트 리스트
        """
        async with self.driver.session() as session:
            try:
                result = await session.run("""
                    MATCH (p:Project)-[:HAS_ENTITY]->(e:Entity)
                    WHERE p.is_public = true
                      AND p.verification_status = 'SUCCESS'
                      AND any(keyword IN $keywords WHERE toLower(e.name) CONTAINS toLower(keyword))
                    WITH p, count(DISTINCT e) as match_count
                    ORDER BY match_count DESC
                    LIMIT $top_k

                    MATCH (p)-[:HAS_ENTITY]->(entity:Entity)
                    OPTIONAL MATCH (entity)-[r:RELATES_TO]->(related:Entity)

                    RETURN p.version_key as version_key,
                           p.name as project_name,
                           collect(DISTINCT {
                               name: entity.name,
                               columns: entity.columns,
                               relations: collect({
                                   to: related.name,
                                   type: r.type
                               })
                           }) as entities
                """, keywords=keywords, top_k=top_k)

                projects = []
                async for record in result:
                    projects.append({
                        "version_key": record["version_key"],
                        "project_name": record["project_name"],
                        "entities": record["entities"],
                        "source": "graph_rag"
                    })

                logger.info(f"Found {len(projects)} similar projects from Graph RAG")
                return projects

            except Exception as e:
                logger.error(f"Failed to search patterns: {e}", exc_info=True)
                return []

    async def health_check(self) -> bool:
        """Neo4j 연결 상태 확인"""
        try:
            async with self.driver.session() as session:
                result = await session.run("RETURN 1 as status")
                await result.single()
                return True
        except Exception as e:
            logger.error(f"Neo4j health check failed: {e}")
            return False


# Singleton 인스턴스 (SingletonMeta로 보장됨)
# 함수 기반 싱글톤에서 메타클래스 기반으로 변경
graph_rag = GraphRAG()
