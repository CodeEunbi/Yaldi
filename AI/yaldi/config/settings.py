from pydantic_settings import BaseSettings
from typing import Literal
from pydantic import field_validator, HttpUrl, AnyUrl
import os


class Settings(BaseSettings):
    # Application
    APP_NAME: str = "Yaldi AI Service"
    APP_ENV: str = "development"
    DEBUG: bool = True
    LOG_LEVEL: str = "INFO"

    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # Spring Boot Backend
    SPRING_BACKEND_URL: str = "http://localhost:8080"
    SPRING_API_KEY: str = ""

    # Test Databases
    TEST_POSTGRES_URL: str = "postgresql://test:test@test-postgres:5432/test_validation"
    TEST_MYSQL_URL: str = "mysql://test:test@test-mysql:3306/test_validation"

    # Neo4j (Graph RAG)
    NEO4J_URI: str = "bolt://neo4j:7687"
    NEO4J_USER: str = "neo4j"
    NEO4J_PASSWORD: str = "yaldi308"

    # LLM APIs (SSAFY GMS)
    GMS_API_KEY: str = "S13P32A308-0ff6c7be-14f8-4eef-8daa-0d580e22b417"  # SSAFY GMS KEY
    GMS_BASE_URL: str = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"  # GMS 엔드포인트
    OPENAI_MODEL: str = "gpt-4o"
    OPENAI_TEMPERATURE: float = 0.7

    DEFAULT_LLM_PROVIDER: Literal["openai", "anthropic", "google"] = "openai"

    # ERD AI Settings
    ERD_MAX_TOKENS: int = 2000
    ERD_TEMPERATURE: float = 0.3

    # CORS
    CORS_ORIGINS: list[str] = ["http://localhost:3000", "http://localhost:5173"]
    CORS_ALLOW_CREDENTIALS: bool = True

    @field_validator('CORS_ORIGINS', mode='before')
    @classmethod
    def parse_cors_origins(cls, v):
        if isinstance(v, str):
            # JSON 형식 또는 쉼표 구분 문자열 지원
            import json
            try:
                return json.loads(v)
            except json.JSONDecodeError:
                return [origin.strip() for origin in v.split(',')]
        return v

    @field_validator('GMS_API_KEY')
    @classmethod
    def validate_gms_api_key(cls, v):
        """GMS API 키는 필수입니다"""
        if not v or v.strip() == "":
            raise ValueError("GMS_API_KEY는 필수입니다. .env 파일에 설정해주세요.")
        return v

    @field_validator('SPRING_API_KEY')
    @classmethod
    def validate_spring_api_key(cls, v):
        """Production 환경에서 Spring API 키는 필수입니다"""
        env = os.getenv('APP_ENV', 'development')
        if env == 'production' and (not v or v.strip() == ""):
            raise ValueError("Production 환경에서 SPRING_API_KEY는 필수입니다.")
        return v

    @field_validator('SPRING_BACKEND_URL', 'GMS_BASE_URL')
    @classmethod
    def validate_http_url(cls, v, info):
        """HTTP/HTTPS URL 형식 검증"""
        if not v:
            raise ValueError(f"{info.field_name}은(는) 필수입니다.")
        if not (v.startswith('http://') or v.startswith('https://')):
            raise ValueError(f"{info.field_name}은(는) http:// 또는 https://로 시작해야 합니다.")
        return v

    @field_validator('NEO4J_URI')
    @classmethod
    def validate_neo4j_uri(cls, v):
        """Neo4j URI 형식 검증 (bolt:// 또는 neo4j://)"""
        if not v:
            raise ValueError("NEO4J_URI는 필수입니다.")
        if not (v.startswith('bolt://') or v.startswith('neo4j://') or v.startswith('neo4j+s://')):
            raise ValueError("NEO4J_URI는 bolt://, neo4j://, 또는 neo4j+s://로 시작해야 합니다.")
        return v

    @field_validator('TEST_POSTGRES_URL', 'TEST_MYSQL_URL')
    @classmethod
    def validate_db_url(cls, v, info):
        """데이터베이스 URL 형식 검증"""
        if not v:
            return v  # 테스트 DB는 선택적

        field_name = info.field_name
        if 'POSTGRES' in field_name and not v.startswith('postgresql://'):
            raise ValueError(f"{field_name}은(는) postgresql://로 시작해야 합니다.")
        elif 'MYSQL' in field_name and not v.startswith('mysql://'):
            raise ValueError(f"{field_name}은(는) mysql://로 시작해야 합니다.")
        return v

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
