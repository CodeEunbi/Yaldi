"""
10개 Expert Agents + GeneralAdviceAgent

각 Agent는 BaseExpertAgent를 상속받아 전문 분야만 다름
"""
from .base_expert import BaseExpertAgent


class NormalizationExpert(BaseExpertAgent):
    """정규화 전문가"""
    def __init__(self):
        super().__init__("NormalizationExpert", "normalization")


class PKSelectionExpert(BaseExpertAgent):
    """PK 선택 전문가"""
    def __init__(self):
        super().__init__("PKSelectionExpert", "pk_selection")


class RelationshipExpert(BaseExpertAgent):
    """관계 설정 전문가"""
    def __init__(self):
        super().__init__("RelationshipExpert", "relationship")


class DataTypeExpert(BaseExpertAgent):
    """데이터 타입 전문가"""
    def __init__(self):
        super().__init__("DataTypeExpert", "data_type")


class ConstraintExpert(BaseExpertAgent):
    """제약 조건 전문가"""
    def __init__(self):
        super().__init__("ConstraintExpert", "constraint")


class DirectionalityExpert(BaseExpertAgent):
    """방향성 전문가"""
    def __init__(self):
        super().__init__("DirectionalityExpert", "directionality")


class ManyToManyExpert(BaseExpertAgent):
    """N:M 관계 전문가"""
    def __init__(self):
        super().__init__("ManyToManyExpert", "many_to_many")


class IndexStrategyExpert(BaseExpertAgent):
    """인덱스 전략 전문가"""
    def __init__(self):
        super().__init__("IndexStrategyExpert", "index_strategy")


class ScalabilityExpert(BaseExpertAgent):
    """확장성 전문가"""
    def __init__(self):
        super().__init__("ScalabilityExpert", "scalability")


class BestPracticeExpert(BaseExpertAgent):
    """베스트 프랙티스 전문가"""
    def __init__(self):
        super().__init__("BestPracticeExpert", "best_practice")


class GeneralAdviceAgent(BaseExpertAgent):
    """일반 조언 Agent (Fallback용)"""
    def __init__(self):
        super().__init__("GeneralAdviceAgent", "general")
