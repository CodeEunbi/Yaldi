import React from 'react';
import MultiSelectIcon from '../../../assets/icons/multi_select_icon.svg?react';

type MultiSelectionProps = {
  onClick?: () => void;
  isActive?: boolean;
};

const MultiSelection: React.FC<MultiSelectionProps> = ({
  onClick,
  isActive,
}) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="다중 선택"
    >
      <MultiSelectIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default MultiSelection;
