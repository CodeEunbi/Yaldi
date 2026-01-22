import React, { useEffect, useMemo, useRef, useState } from 'react';
import type { WorkspaceTable } from '../../../types/tableCard';
import { TABLE_COLOR_OPTIONS } from '../constants/colorOptions';
import type { TableActionButton } from '../../../types/tableCard';

type TableCardActionButtonsProps = {
  tableId: string;
  tableColor: WorkspaceTable['color'];
  actions: TableActionButton[];
  isSelected: boolean;
  isEditingDisabled: boolean;
  onUpdateColor: (tableId: string, color: WorkspaceTable['color']) => void;
};

const TableCardActionButtons: React.FC<TableCardActionButtonsProps> = ({
  tableId,
  tableColor,
  actions,
  isSelected,
  isEditingDisabled,
  onUpdateColor,
}) => {
  const paletteRef = useRef<HTMLDivElement | null>(null);
  const [isPaletteOpen, setIsPaletteOpen] = useState(false);

  const actionableButtons = useMemo(
    () => actions.filter((action) => action.key !== 'create-memo'),
    [actions],
  );

  useEffect(() => {
    if (!isSelected) {
      setIsPaletteOpen(false);
    }
  }, [isSelected]);

  useEffect(() => {
    if (!isPaletteOpen) {
      return;
    }

    const handleDocumentClick = (event: MouseEvent) => {
      if (
        paletteRef.current &&
        !paletteRef.current.contains(event.target as Node)
      ) {
        setIsPaletteOpen(false);
      }
    };

    document.addEventListener('click', handleDocumentClick);
    return () => {
      document.removeEventListener('click', handleDocumentClick);
    };
  }, [isPaletteOpen]);

  if (!isSelected || actionableButtons.length === 0) {
    return null;
  }

  return (
    <div className="absolute -top-10 left-2 z-40">
      <div className="relative flex gap-1">
        {actionableButtons.map(({ key, Icon, bgClass, label, onClick }) => (
          <button
            key={key}
            type="button"
            disabled={isEditingDisabled}
            onClick={(event) => {
              event.stopPropagation();
              if (isEditingDisabled) {
                return;
              }
              if (key === 'change-color') {
                setIsPaletteOpen((prev) => !prev);
                return;
              }
              setIsPaletteOpen(false);
              onClick?.(tableId);
            }}
            className={`flex h-8 w-8 items-center justify-center rounded-md shadow ${bgClass} ${
              isEditingDisabled ? 'cursor-not-allowed' : ''
            }`}
            aria-label={label}
          >
            <Icon className="h-[20px] w-[20px]" />
          </button>
        ))}
        {isPaletteOpen ? (
          <div
            ref={paletteRef}
            className="absolute top-10 left-0 grid grid-cols-4 gap-2 rounded-lg border border-my-border bg-white p-2 shadow-lg"
            onClick={(event) => event.stopPropagation()}
          >
            {TABLE_COLOR_OPTIONS.map(({ key, value }) => {
              const isActive = tableColor === value;
              return (
                <button
                  key={key}
                  type="button"
                  title={key}
                  aria-label={`${key} 색상 선택`}
                  onClick={(event) => {
                    event.stopPropagation();
                    onUpdateColor(tableId, value);
                    setIsPaletteOpen(false);
                  }}
                  className={`h-8 w-8 rounded-md border-2 transition-transform duration-150 ${
                    isActive
                      ? 'scale-105 border-blue'
                      : 'border-transparent hover:scale-105'
                  } bg-${value}`}
                />
              );
            })}
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default React.memo(TableCardActionButtons);
