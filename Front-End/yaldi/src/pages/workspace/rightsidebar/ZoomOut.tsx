import React from 'react';
import ZoomOutIcon from '../../../assets/icons/zoom-out-icon.svg?react';

interface ZoomOutProps {
  onClick?: () => void;
  disabled?: boolean;
}

const ZoomOut: React.FC<ZoomOutProps> = ({ onClick, disabled = false }) => {
  const handleClick = () => {
    if (disabled) {
      return;
    }
    if (onClick) {
      onClick();
    } else {
      console.log('zoom-out 아이콘 클릭');
    }
  };

  return (
    <button
      onClick={handleClick}
      disabled={disabled}
      className={`w-8 h-8 flex items-center justify-center rounded transition-colors ${
        disabled
          ? 'cursor-not-allowed opacity-40'
          : 'hover:bg-ai-chat focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ai-chat'
      }`}
      aria-label="축소"
    >
      <ZoomOutIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default ZoomOut;
