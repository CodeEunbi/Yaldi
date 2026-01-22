import React from 'react';
import RelationIcon1 from '../../../assets/icons/relation-one.svg?react';

type Relation1Props = {
  onClick?: () => void;
  isActive?: boolean;
};

const Relation1: React.FC<Relation1Props> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="1 relation"
    >
      <RelationIcon1 className="w-6 h-6 text-my-black" />
    </button>
  );
};

export default Relation1;

