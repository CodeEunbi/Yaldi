import React from 'react';
import EntityIcon from '../../../assets/icons/entity-icon.svg?react';

interface EntityProps {
  onClick?: () => void;
  isActive?: boolean;
  disabled?: boolean;
}

const Entity: React.FC<EntityProps> = ({
  onClick,
  isActive = false,
  disabled = false,
}) => {
  const handleClick = () => {
    if (disabled) {
      return;
    }

    if (onClick) {
      onClick();
    } else {
      console.log('entity 아이콘 클릭');
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={disabled}
      className={`w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors border ${
        isActive
          ? 'bg-blue/10 border-blue text-blue'
          : 'border-transparent hover:bg-ai-chat text-my-black'
      } ${disabled ? 'cursor-not-allowed opacity-40' : ''}`}
      aria-label="Entity"
      aria-pressed={isActive}
      title="엔티티"
    >
      <EntityIcon
        className={`w-5 h-5 ${isActive ? 'text-blue' : 'text-my-black'}`}
      />
    </button>
  );
};

export default Entity;
