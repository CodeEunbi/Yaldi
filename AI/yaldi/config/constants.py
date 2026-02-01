"""
애플리케이션 상수 정의

하드코딩된 매직 넘버를 한 곳에서 관리
"""


class AgentConstants:
    """
    AI Agent 관련 상수

    모든 Agent에서 사용하는 threshold, 반복 횟수 등을 정의
    """

    # Intent Router
    INTENT_CONFIDENCE_THRESHOLD = 0.6
    """Intent Router에서 Expert Agent 실행 여부를 결정하는 신뢰도 임계값"""

    # ERD Generation Workflow
    SIMILARITY_THRESHOLD = 0.33
    """Graph RAG에서 REFERENCE 모드로 전환하는 유사도 임계값 (1/3)"""

    MAX_REFINEMENT_ITERATIONS = 3
    """스키마 검증 실패 시 최대 재시도 횟수"""

    # Mock Data Agent
    MAX_MOCK_DATA_RETRIES = 3
    """Mock Data SQL 생성 실패 시 최대 재시도 횟수"""

    # Temperature 기본값
    DEFAULT_TEMPERATURE = 0.7
    """LLM 기본 temperature 값"""

    CLASSIFICATION_TEMPERATURE = 0.0
    """분류 작업용 temperature (deterministic)"""

    EXPERT_TEMPERATURE = 0.3
    """Expert Agent용 temperature (창의성과 일관성 균형)"""

    ANALYSIS_TEMPERATURE = 0.1
    """분석 작업용 temperature (정확성 우선)"""


class WorkflowConstants:
    """
    Workflow 관련 상수
    """

    # Graph RAG
    GRAPH_RAG_TOP_K = 3
    """Graph RAG 검색 시 반환할 최대 유사 프로젝트 수"""

    # Conversation
    MAX_CONVERSATION_HISTORY = 20
    """대화 히스토리 최대 저장 턴 수"""

    RECENT_CONTEXT_TURNS = 3
    """Intent Router에 전달할 최근 대화 턴 수"""


class LLMConstants:
    """
    LLM 관련 상수
    """

    # Token Limits
    DEFAULT_MAX_TOKENS = 4000
    """기본 최대 토큰 수"""

    ERD_MAX_TOKENS = 2000
    """ERD 생성 시 최대 토큰 수"""

    MOCK_DATA_MAX_TOKENS = 4000
    """Mock Data 생성 시 최대 토큰 수"""

    # Embedding
    EMBEDDING_MODEL = "text-embedding-3-small"
    """기본 임베딩 모델"""

    EMBEDDING_DIMENSION = 1536
    """text-embedding-3-small 차원 수"""


class DatabaseConstants:
    """
    데이터베이스 관련 상수
    """

    # 지원 DB 타입
    SUPPORTED_DB_TYPES = ["postgresql", "mysql"]
    """지원하는 데이터베이스 타입"""

    DEFAULT_DB_TYPE = "postgresql"
    """기본 데이터베이스 타입"""


# 하위 호환성을 위한 별칭
INTENT_CONFIDENCE_THRESHOLD = AgentConstants.INTENT_CONFIDENCE_THRESHOLD
SIMILARITY_THRESHOLD = AgentConstants.SIMILARITY_THRESHOLD
MAX_REFINEMENT_ITERATIONS = AgentConstants.MAX_REFINEMENT_ITERATIONS
MAX_MOCK_DATA_RETRIES = AgentConstants.MAX_MOCK_DATA_RETRIES
