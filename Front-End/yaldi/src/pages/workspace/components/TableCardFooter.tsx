import React from 'react';

type TableCardFooterProps = {
  isVisible: boolean;
  onApply: () => void;
  onCancel: () => void;
};

const TableCardFooter: React.FC<TableCardFooterProps> = ({
  isVisible,
  onApply,
  onCancel,
}) => {
  if (!isVisible) return null;

  return (
    <div className="absolute right-0 top-full mt-2 flex gap-2 z-50">
      <button
        onClick={(e) => {
          e.stopPropagation();
          onApply();
        }}
        className="rounded-md bg-[#1f3f9d] px-4 py-1.5 text-sm font-medium text-white hover:bg-[#1a3380] transition-colors shadow-sm"
      >
        적용
      </button>
      <button
        onClick={(e) => {
          e.stopPropagation();
          onCancel();
        }}
        className="rounded-md border border-gray-300 bg-white px-4 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors shadow-sm"
      >
        취소
      </button>
    </div>
  );
};

export default TableCardFooter;
