import React from 'react';
import ImportIcon from '../../../assets/icons/import_icon.svg?react';

interface ImportProps {
  onClick?: () => void;
  isActive?: boolean;
}

const Import: React.FC<ImportProps> = ({ onClick, isActive }) => {
  const handleClick = () => {
    if (onClick) {
      onClick();
    } else {
      console.log('import 아이콘 클릭');
    }
  };

  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      onClick={handleClick}
      className={buttonClasses}
      aria-label="임포트"
    >
      <ImportIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default Import;
