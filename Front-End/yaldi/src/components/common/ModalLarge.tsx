import React, { useEffect } from 'react';
import CloseIcon from '../../assets/icons/close_icon.svg?react';

// 모든 표준 HTML div 속성을 상속받도록 정의
interface ModalProps extends React.ComponentPropsWithoutRef<'div'> {
  // 모달의 열림/닫힘 상태 (필수)
  isOpen: boolean;

  // 모달을 닫는 함수 (필수)
  onClose: () => void;

  // 모달 내부에 들어갈 내용
  children: React.ReactNode;

  // 모달의 제목 (선택)
  title?: string;
}

const ModalLarge: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  children,
  title,
  className = '', // 모달 본체에 추가할 클래스
  ...rest
}) => {

  // 🚀 화면 흔들림 방지 및 스크롤바 제어 로직 수정
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const bodyStyle = document.body.style;
    const originalPaddingRight = bodyStyle.paddingRight;
    const originalOverflow = bodyStyle.overflow;

    if (isOpen) {
      // 1. 스크롤바 너비 계산: 뷰포트 너비 - HTML 문서 너비
      const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
      // 2. 스크롤 숨김 및 보정 패딩 적용
      bodyStyle.overflow = 'hidden';
      bodyStyle.paddingRight = `${scrollbarWidth}px`;
    }

    // 클린업 함수: 닫힐 때 또는 언마운트 시 원래 상태로 복원
    return () => {
      bodyStyle.overflow = originalOverflow;
      bodyStyle.paddingRight = originalPaddingRight;
    };
  }, [isOpen]); // isOpen 상태가 변할 때마다 실행



  // 💡 팁: 모달이 열릴 때 body 스크롤을 막는 로직을 여기에 추가할 수 있습니다.
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset'; // 클린업 함수
    };
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  // 오버레이 클릭 핸들러 (모달 본체가 아닌 바깥 영역 클릭 시 닫기)
  const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
    // 이벤트 버블링을 통해 오버레이 자체를 클릭했는지 확인 (모달 본체 클릭 제외)
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    // 모달 오버레이: fixed, 전체 화면, 배경 어둡게
    // onClick으로 오버레이를 클릭했을 때 닫기 핸들러 실행
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 backdrop-blur-sm"
      onClick={handleOverlayClick}
      {...rest}
    >
      {/* 모달 본체: 흰색 배경, 그림자 */}
      <div
        className={`w-[900px] px-5 pb-2.5 relative bg-my-white rounded-[20px] inline-flex flex-col justify-start items-start gap-2.5 shadow-2xl max-h-[90vh] overflow-y-auto transform transition-all ${className}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={title ? "modal-title" : undefined}
        style={{
          scrollbarWidth: 'none',
          msOverflowStyle: 'none',
        }}
      >
        {/* 모달 헤더 */}
        <div className="sticky top-0 right-0 left-0 self-stretch pt-5 pb-2.5 flex border-b border-my-border justify-between items-center bg-my-white ">
          <h3 id="modal-title" className="justify-center text-my-black text-2xl font-bold font-['Pretendard']">
            {title || '모달 제목'}
          </h3>
          <button
            data-svg-wrapper
            onClick={onClose}
            className="text-my-black hover:text-gray-600 transition-colors"
            aria-label="모달 닫기"
          >
            {/* Tailwind를 이용한 닫기(X) 아이콘 */}
            <CloseIcon />
          </button>
        </div>

        {/* 모달 내용 */}
        <div className="self-stretch gap-2.5 p-2.5">
          {children}
        </div>

      </div>
    </div>
  );
};

export default ModalLarge;