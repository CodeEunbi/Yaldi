import React from 'react';
import ExportIcon from '../../../assets/icons/export_icon.svg?react';

interface ExportProps {
  onClick?: () => void;
  isActive?: boolean;
}

const Export: React.FC<ExportProps> = ({ onClick, isActive }) => {
  const handleClick = () => {
    if (onClick) {
      onClick();
    } else {
      console.log('export 아이콘 클릭');
    }
  };

  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      onClick={handleClick}
      className={buttonClasses}
      aria-label="익스포트"
    >
      <ExportIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default Export;
