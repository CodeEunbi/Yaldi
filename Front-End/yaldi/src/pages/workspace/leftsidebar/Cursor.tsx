import React from 'react';
import CursorIcon from '../../../assets/icons/cursor_icon.svg?react';

type CursorProps = {
  onClick?: () => void;
  isActive?: boolean;
};

const Cursor: React.FC<CursorProps> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="커서"
    >
      <CursorIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default Cursor;
