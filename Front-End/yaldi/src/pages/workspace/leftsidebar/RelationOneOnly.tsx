import React from 'react';
import RelationIcon11 from '../../../assets/icons/relation-one-only.svg?react';

type Relation11Props = {
  onClick?: () => void;
  isActive?: boolean;
};

const Relation11: React.FC<Relation11Props> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="11 relation"
    >
      <RelationIcon11 className="w-6 h-6 text-my-black" />
    </button>
  );
};

export default Relation11;
