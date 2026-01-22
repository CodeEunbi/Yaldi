import React from 'react';
import ChatIcon from '../../../assets/icons/chat-icon.svg?react';
import ChatBox from '../../../components/common/ChatBox';

interface ChatProps {
  isOpen: boolean;
  onToggle?: () => void;
  onClose: () => void;
  dimmed?: boolean;
}

const Chat: React.FC<ChatProps> = ({
  isOpen,
  onToggle,
  onClose,
  dimmed = false,
}) => {
  const handleClick = () => {
    onToggle?.();
  };

  return (
    <>
      <button
        onClick={handleClick}
        className="w-8 h-8 flex items-center justify-center mb-2 hover:bg-ai-chat rounded transition-colors"
        aria-label="댓글"
      >
        <ChatIcon className="w-5 h-5 text-my-black" />
      </button>

      <ChatBox isOpen={isOpen} onClose={onClose} title="댓글" dimmed={dimmed}>
        <div className="flex flex-col gap-4">
          <article className="border border-my-border rounded-lg p-3 bg-gray-50">
            <header className="flex items-center justify-between mb-2">
              <div>
                <p className="text-sm font-semibold text-my-black font-pretendard">
                  홍길동
                </p>
                <p className="text-xs text-my-gray-500 font-pretendard">
                  오늘 오전 9:12 · 도면 #01
                </p>
              </div>
              <span className="text-xs text-my-gray-400 font-pretendard">
                미확인
              </span>
            </header>
            <p className="text-sm text-my-black font-pretendard leading-relaxed">
              가장자리 박스 높이를 6px 낮춰보는 게 어떨까요? 시안 B 참고
              부탁드려요.
            </p>
          </article>

          <article className="border border-my-border rounded-lg p-3">
            <header className="flex items-center justify-between mb-2">
              <div>
                <p className="text-sm font-semibold text-my-black font-pretendard">
                  최유진
                </p>
                <p className="text-xs text-my-gray-500 font-pretendard">
                  어제 오후 4:37 · 인터랙션
                </p>
              </div>
              <span className="text-xs text-blue-500 font-pretendard">
                확인됨
              </span>
            </header>
            <p className="text-sm text-my-black font-pretendard leading-relaxed">
              스크롤 시 헤더 그림자가 너무 진한 것 같아요. 다음 배포 전 톤
              다운해주세요.
            </p>
            <footer className="mt-3 flex items-center gap-3 text-xs text-my-gray-400 font-pretendard">
              <button type="button" className="hover:underline">
                답글 달기
              </button>
              <span>·</span>
              <span>2개의 답글</span>
            </footer>
          </article>
        </div>
      </ChatBox>
    </>
  );
};

export default Chat;
