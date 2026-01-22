import React from 'react';
import RelationIcon011 from '../../../assets/icons/relation-zero-or-many.svg?react';

type Relation011Props = {
  onClick?: () => void;
  isActive?: boolean;
};

const Relation011: React.FC<Relation011Props> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="01 relation 1"
    >
      <RelationIcon011 className="w-6 h-6 text-my-black" />
    </button>
  );
};

export default Relation011;
