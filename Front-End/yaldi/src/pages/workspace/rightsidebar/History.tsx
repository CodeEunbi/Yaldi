import React from 'react';
import HistoryIcon from '../../../assets/icons/history.svg?react';
import ChatPanel from '../../../components/common/ChatBox';

interface HistoryProps {
  isOpen: boolean;
  onToggle?: () => void;
  onClose: () => void;
  dimmed?: boolean;
}

const History: React.FC<HistoryProps> = ({
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
        aria-label="히스토리"
      >
        <HistoryIcon className="w-5 h-5 text-my-black" />
      </button>

      <ChatPanel
        isOpen={isOpen}
        onClose={onClose}
        title="히스토리"
        dimmed={dimmed}
      >
        <div className="flex flex-col gap-4">
          <p className="text-sm text-my-gray-500 font-pretendard">
            최근 작업 기록이 여기에 표시됩니다.
          </p>

          <ul className="flex flex-col gap-3">
            <li className="text-sm text-my-black font-pretendard">
              • 예시 변경 사항 1
            </li>
            <li className="text-sm text-my-black font-pretendard">
              • 예시 변경 사항 2
            </li>
            <li className="text-sm text-my-black font-pretendard">
              • 예시 변경 사항 3
            </li>
          </ul>
        </div>
      </ChatPanel>
    </>
  );
};

export default History;
