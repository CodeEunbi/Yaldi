import React from 'react';
import CloseIcon from '../../assets/icons/close_icon.svg?react';

interface ChatProps {
  isOpen: boolean;
  onClose: () => void;
  children?: React.ReactNode;
  /** 헤더 높이(px) */
  headerHeight?: number;
  /** 패널 가로 길이(px) */
  width?: number;
  /** 하단 여백(px) */
  bottomOffset?: number;
  /** 우측 사이드바 가로 길이(px) */
  sidebarOffset?: number;
  /** 상단 영역 좌측에 표시할 제목 (선택) */
  title?: string;
  footer?: React.ReactNode;
  /** 외부 모달 등으로 패널을 흐릿하게 표시할지 여부 */
  dimmed?: boolean;
}

const Chat: React.FC<ChatProps> = ({
  isOpen,
  onClose,
  children,
  headerHeight = 80,
  width = 350,
  bottomOffset = 24,
  sidebarOffset = 64,
  title,
  footer,
  dimmed = false,
}) => {
  if (!isOpen) {
    return null;
  }

  const panelStyle: React.CSSProperties = {
    top: headerHeight,
    right: sidebarOffset,
    bottom: bottomOffset,
    width,
  };

  const handlePanelClick = (event: React.MouseEvent<HTMLDivElement>) => {
    event.stopPropagation();
  };

  const panelClassName = [
    'absolute bg-my-white border border-my-border shadow-2xl rounded-tl-[24px] rounded-tr-[16px] rounded-br-[16px] rounded-bl-[24px] overflow-hidden transition-all duration-150 flex flex-col pointer-events-auto',
  ]
    .join(' ')
    .trim();

  const contentWrapperClassName = [
    'relative z-20 flex flex-col h-full',
    dimmed ? 'pointer-events-none blur-sm' : '',
  ]
    .join(' ')
    .trim();

  return (
    <div
      className="fixed inset-0 z-40 flex justify-end pointer-events-none"
      role="dialog"
      aria-modal="true"
    >
      <div
        className="flex-1 pointer-events-auto"
        onClick={onClose}
        aria-hidden
      />
      <div
        className="relative pointer-events-none"
        style={{ width: width + sidebarOffset }}
      >
        <div
          className={panelClassName}
          style={panelStyle}
          onClick={handlePanelClick}
        >
          {dimmed && (
            <div className="absolute inset-0 bg-white/80 backdrop-blur-sm z-10" />
          )}

          <div className={contentWrapperClassName}>
            <div className="flex items-center justify-between h-12 px-4 border-b border-my-border">
              {title ? (
                <h2 className="text-base font-semibold text-my-black font-pretendard">
                  {title}
                </h2>
              ) : (
                <span className="sr-only">채팅</span>
              )}
              <button
                type="button"
                onClick={onClose}
                className="w-8 h-8 flex items-center justify-center rounded-full hover:bg-gray-100 active:bg-gray-200 transition-colors"
                aria-label="닫기"
              >
                <CloseIcon className="w-4 h-4 text-my-black" />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto px-4 py-3">
              {children ?? (
                <p className="text-sm text-my-gray-500 font-pretendard">
                  아직 채팅 메시지가 없습니다.
                </p>
              )}
            </div>
            {footer && (
              <div className="border-t border-my-border bg-my-white px-3 py-3">
                {footer}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Chat;
