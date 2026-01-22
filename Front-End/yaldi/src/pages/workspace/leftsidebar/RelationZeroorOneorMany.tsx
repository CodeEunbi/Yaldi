import React from 'react';
import RelationIcon013 from '../../../assets/icons/relation-zero-or-one-or-many.svg?react';

type Relation013Props = {
  onClick?: () => void;
  isActive?: boolean;
};

const Relation013: React.FC<Relation013Props> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="013 relation"
    >
      <RelationIcon013 className="w-6 h-6 text-my-black" />
    </button>
  );
};

export default Relation013;
