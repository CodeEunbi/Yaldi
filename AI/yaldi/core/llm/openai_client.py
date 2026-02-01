from openai import AsyncOpenAI
from config.settings import settings
from core.singleton import SingletonMeta
from typing import List, Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)


class OpenAIClient(metaclass=SingletonMeta):
    """
    OpenAI API 클라이언트 (SSAFY GMS 사용)

    Singleton 패턴 적용: 애플리케이션 전체에서 하나의 인스턴스만 생성됨
    """

    def __init__(self):
        # Singleton이므로 중복 초기화 방지
        if hasattr(self, '_initialized'):
            return
        self._initialized = True

        self.client = AsyncOpenAI(
            api_key=settings.GMS_API_KEY,
            base_url=settings.GMS_BASE_URL
        )
        self.model = settings.OPENAI_MODEL
        self.default_temperature = settings.OPENAI_TEMPERATURE

        logger.info("OpenAIClient singleton instance created")

    async def chat_completion(
        self,
        messages: List[Dict[str, str]],
        temperature: Optional[float] = None,
        max_tokens: Optional[int] = None,
        response_format: Optional[Dict[str, str]] = None
    ) -> str:
        """
        Chat Completion API 호출

        Args:
            messages: 대화 메시지 리스트 [{"role": "user", "content": "..."}]
            temperature: 온도 설정 (기본값: settings.OPENAI_TEMPERATURE)
            max_tokens: 최대 토큰 수 (기본값: None)
            response_format: 응답 형식 (예: {"type": "json_object"})

        Returns:
            LLM 응답 텍스트
        """
        try:
            logger.info(f"Calling GMS API - Endpoint: {settings.GMS_BASE_URL}, Model: {self.model}")
            logger.debug(f"Messages: {messages}")

            kwargs = {
                "model": self.model,
                "messages": messages,
                "temperature": temperature if temperature is not None else self.default_temperature,
            }

            if max_tokens is not None:
                kwargs["max_tokens"] = max_tokens

            if response_format is not None:
                kwargs["response_format"] = response_format

            response = await self.client.chat.completions.create(**kwargs)

            result = response.choices[0].message.content
            logger.info(f"✅ GMS API call successful!")
            logger.info(f"Tokens used - Prompt: {response.usage.prompt_tokens}, "
                       f"Completion: {response.usage.completion_tokens}, "
                       f"Total: {response.usage.total_tokens}")
            logger.debug(f"AI Response: {result}")

            return result

        except Exception as e:
            logger.error(f"❌ GMS API error: {e}")
            logger.error(f"Error type: {type(e).__name__}")
            raise

    async def json_completion(
        self,
        messages: List[Dict[str, str]],
        temperature: Optional[float] = None,
        max_tokens: Optional[int] = None
    ) -> Dict[str, Any]:
        """
        JSON 형식으로 응답받는 Chat Completion

        Args:
            messages: 대화 메시지 리스트
            temperature: 온도 설정
            max_tokens: 최대 토큰 수

        Returns:
            파싱된 JSON 딕셔너리
        """
        import json

        result = await self.chat_completion(
            messages=messages,
            temperature=temperature,
            max_tokens=max_tokens,
            response_format={"type": "json_object"}
        )

        return json.loads(result)

    async def create_embedding(
        self,
        text: str,
        model: str = "text-embedding-3-small"
    ) -> List[float]:
        """
        텍스트 임베딩 생성

        Args:
            text: 임베딩할 텍스트
            model: 임베딩 모델 (기본값: text-embedding-3-small)

        Returns:
            임베딩 벡터 (float 리스트)
        """
        try:
            logger.info(f"Creating embedding - Model: {model}, Text length: {len(text)}")

            response = await self.client.embeddings.create(
                model=model,
                input=text
            )

            embedding = response.data[0].embedding
            logger.info(f"✅ Embedding created successfully - Dimension: {len(embedding)}")
            logger.info(f"Tokens used: {response.usage.total_tokens}")

            return embedding

        except Exception as e:
            logger.error(f"❌ Embedding creation error: {e}")
            logger.error(f"Error type: {type(e).__name__}")
            raise



# Singleton 인스턴스 (SingletonMeta로 보장됨)
# 어디서든 동일한 인스턴스 반환
openai_client = OpenAIClient()
