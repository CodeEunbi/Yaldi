import React from 'react';
import AddTableIcon from '../../../assets/icons/add_table_icon.svg?react';

type AddTableProps = {
  onClick?: () => void;
  isActive?: boolean;
};

const AddTable: React.FC<AddTableProps> = ({ onClick, isActive }) => {
  const buttonClasses = `w-8 h-8 flex items-center justify-center mb-2 rounded transition-colors hover:bg-ai-chat ${
    isActive ? 'bg-ai-chat' : 'bg-transparent'
  }`;

  return (
    <button
      type="button"
      onClick={onClick}
      className={buttonClasses}
      aria-label="테이블 추가"
    >
      <AddTableIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default AddTable;
