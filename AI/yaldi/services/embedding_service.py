from core.llm.openai_client import openai_client
from typing import Dict, List, Optional
import logging

logger = logging.getLogger(__name__)


class EmbeddingService:
    """가중치 기반 임베딩 서비스"""

    ## 가중치를 다르게 둠
    DEFAULT_WEIGHTS = {
        "sql": 0.5,              # SQL 50% 
        "project_name": 0.25,    # 프로젝트명 25%
        "project_desc": 0.15,    # 프로젝트 설명 15%
        "version_name": 0.07,    # 버전명 7%
        "version_desc": 0.03     # 버전 설명 3%
    }

    async def create_weighted_embedding(
        self,
        fields: Dict[str, Optional[str]],
        weights: Optional[Dict[str, float]] = None
    ) -> List[float]:
        """
        필드별 임베딩 후 가중치 합성

        Args:
            fields: 각 필드의 텍스트
                {
                    "project_name": "...",
                    "project_desc": "...",
                    "version_name": "...",
                    "version_desc": "...",
                    "sql": "..."
                }
            weights: 각 필드의 가중치 (합=1.0)

        Returns:
            가중치 합성된 임베딩 벡터
        """

        if weights is None:
            weights = self.DEFAULT_WEIGHTS.copy()

        logger.info(f"가중치 기반 임베딩 생성 시작 - weights: {weights}")

        # 1. 각 필드별로 임베딩 생성
        embeddings = {}
        for field_name, text in fields.items():
            if not text or not text.strip():
                logger.debug(f"필드 {field_name} 비어있음, 스킵")
                continue

            weight = weights.get(field_name, 0)
            if weight == 0:
                logger.debug(f"필드 {field_name} 가중치 0, 스킵")
                continue

            logger.info(f"필드 {field_name} 임베딩 생성 중 (가중치: {weight})")
            embedding = await openai_client.create_embedding(text)
            embeddings[field_name] = {
                "vector": embedding,
                "weight": weight
            }

        if not embeddings:
            raise ValueError("임베딩할 필드가 없습니다")

        # 2. 가중치 합성
        vector_length = len(list(embeddings.values())[0]["vector"])
        final_vector = [0.0] * vector_length

        for field_name, data in embeddings.items():
            weight = data["weight"]
            vector = data["vector"]

            for i in range(vector_length):
                final_vector[i] += weight * vector[i]

        return final_vector


embedding_service = EmbeddingService()
