import React from 'react';
import type { RelationType } from '../WorkSpace';

type RelationTypeModalProps = {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (type: RelationType) => void;
};

const RelationTypeModal: React.FC<RelationTypeModalProps> = ({
  isOpen,
  onClose,
  onSelect,
}) => {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-xl font-bold mb-4 text-gray-800">
          관계선 타입 선택
        </h2>
        <p className="text-sm text-gray-600 mb-6">
          생성할 관계선의 타입을 선택해주세요
        </p>

        <div className="space-y-3">
          <button
            type="button"
            onClick={() => onSelect('identifying')}
            className="w-full p-4 border-2 border-gray-300 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-all text-left group"
          >
            <div className="flex items-center justify-between">
              <div>
                <div className="font-semibold text-gray-800 group-hover:text-blue-600">
                  식별 관계 (Identifying)
                </div>
                <div className="text-sm text-gray-600 mt-1">
                  실선으로 표시됩니다
                </div>
              </div>
              <div className="w-16 h-0.5 bg-gray-800 group-hover:bg-blue-600" />
            </div>
          </button>

          <button
            type="button"
            onClick={() => onSelect('non-identifying')}
            className="w-full p-4 border-2 border-gray-300 rounded-lg hover:border-green-500 hover:bg-green-50 transition-all text-left group"
          >
            <div className="flex items-center justify-between">
              <div>
                <div className="font-semibold text-gray-800 group-hover:text-green-600">
                  비식별 관계 (Non-identifying)
                </div>
                <div className="text-sm text-gray-600 mt-1">
                  점선으로 표시됩니다
                </div>
              </div>
              <div
                className="w-16 h-0.5 bg-gray-800 group-hover:bg-green-600"
                style={{
                  backgroundImage:
                    'repeating-linear-gradient(to right, currentColor 0, currentColor 4px, transparent 4px, transparent 8px)',
                  backgroundColor: 'transparent',
                }}
              />
            </div>
          </button>
        </div>

        <button
          type="button"
          onClick={onClose}
          className="mt-6 w-full py-2 px-4 bg-gray-200 hover:bg-gray-300 rounded-lg transition-colors text-gray-800 font-medium"
        >
          취소
        </button>
      </div>
    </div>
  );
};

export default RelationTypeModal;
