import React from 'react';
import RelationIcon13 from '../../../assets/icons/relation-one-or-many.svg?react';

type Relation13Props = {
  onClick?: () => void;
  isActive?: boolean;
};

const Relation13: React.FC<Relation13Props> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="13 relation"
    >
      <RelationIcon13 className="w-6 h-6 text-my-black" />
    </button>
  );
};

export default Relation13;
