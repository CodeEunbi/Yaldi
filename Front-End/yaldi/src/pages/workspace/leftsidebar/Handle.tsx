import React from 'react';
import HandIcon from '../../../assets/icons/hand_icon.svg?react';

type HandleProps = {
  onClick?: () => void;
  isActive?: boolean;
};

const Handle: React.FC<HandleProps> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="손"
    >
      <HandIcon className="w-6 h-6 text-my-black" />
    </button>
  );
};

export default Handle;
