import React from 'react';
import RelationIcon01 from '../../../assets/icons/relation-zero-or-one.svg?react';

type Relation01Props = {
  onClick?: () => void;
  isActive?: boolean;
};

const Relation01: React.FC<Relation01Props> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="01 relation"
    >
      <RelationIcon01 className="w-6 h-6 text-my-black" />
    </button>
  );
};

export default Relation01;
