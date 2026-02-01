"""
Singleton 패턴 구현

모든 싱글톤 클래스에서 사용할 클래스 제공
"""
from typing import Dict, Any


class SingletonMeta(type):
    """
    Singleton 패턴을 위한 클래스

    사용 예시:
        class MyClass(metaclass=SingletonMeta):
            def __init__(self):
                self.value = 0

        # 항상 같은 인스턴스 반환
        instance1 = MyClass()
        instance2 = MyClass()
        assert instance1 is instance2  # True
    """

    _instances: Dict[type, Any] = {}
    _lock = None  # 필요시 threading.Lock() 사용

    def __call__(cls, *args, **kwargs):
        """
        클래스 호출 시 싱글톤 인스턴스 반환

        Args:
            *args: 클래스 생성자 위치 인자
            **kwargs: 클래스 생성자 키워드 인자

        Returns:
            해당 클래스의 싱글톤 인스턴스
        """
        if cls not in cls._instances:
            # 처음 호출 시에만 인스턴스 생성
            instance = super().__call__(*args, **kwargs)
            cls._instances[cls] = instance

        return cls._instances[cls]

    @classmethod
    def clear_instances(mcs):
        """
        모든 싱글톤 인스턴스 초기화 (테스트용)

        주의: 프로덕션 코드에서는 사용하지 마세요
        """
        mcs._instances.clear()

    @classmethod
    def clear_instance(mcs, cls: type):
        """
        특정 클래스의 싱글톤 인스턴스만 초기화 (테스트용)

        Args:
            cls: 초기화할 클래스

        주의: 프로덕션 코드에서는 사용하지 마세요
        """
        if cls in mcs._instances:
            del mcs._instances[cls]
