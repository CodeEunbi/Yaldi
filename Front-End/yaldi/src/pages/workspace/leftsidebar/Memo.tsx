import React from 'react';
import MemoIcon from '../../../assets/icons/memo_icon.svg?react';

type MemoProps = {
  onClick?: () => void;
  isActive?: boolean;
};

const Memo: React.FC<MemoProps> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="메모"
    >
      <MemoIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default Memo;
