import React from 'react';
import ZoomInIcon from '../../../assets/icons/zoom-in-icon.svg?react';

interface ZoomInProps {
  onClick?: () => void;
  disabled?: boolean;
}

const ZoomIn: React.FC<ZoomInProps> = ({ onClick, disabled = false }) => {
  const handleClick = () => {
    if (disabled) {
      return;
    }
    if (onClick) {
      onClick();
    } else {
      console.log('zoom-in 아이콘 클릭');
    }
  };

  return (
    <button
      onClick={handleClick}
      disabled={disabled}
      className={`w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors ${
        disabled
          ? 'cursor-not-allowed opacity-40'
          : 'hover:bg-ai-chat focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ai-chat'
      }`}
      aria-label="확대"
    >
      <ZoomInIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default ZoomIn;
