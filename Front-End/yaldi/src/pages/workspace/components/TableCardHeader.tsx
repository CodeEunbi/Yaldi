import React, { useCallback, useEffect, useRef, useState } from 'react';
import type { WorkspaceTable } from '../../../types/tableCard';

type TableCardHeaderProps = {
  table: WorkspaceTable;
  isEditingDisabled: boolean;
  headerBorderClass: string;
  headerBgClass: string;
  headerTextClass: string;
  subTextClass: string;
  onUpdateName: (tableId: string, name: string) => void;
  onUpdateIdentifier: (tableId: string, identifier: string) => void;
  onHeaderMouseDown: (event: React.MouseEvent<HTMLDivElement>) => void;
};

const TableCardHeader: React.FC<TableCardHeaderProps> = ({
  table,
  isEditingDisabled,
  headerBorderClass,
  headerBgClass,
  headerTextClass,
  subTextClass,
  onUpdateName,
  onUpdateIdentifier,
  onHeaderMouseDown,
}) => {
  const [isEditingName, setIsEditingName] = useState(false);
  const [nameValue, setNameValue] = useState(table.name);
  const [isEditingIdentifier, setIsEditingIdentifier] = useState(false);
  const [identifierValue, setIdentifierValue] = useState(table.identifier);

  const nameInputRef = useRef<HTMLInputElement | null>(null);
  const identifierInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setNameValue(table.name);
  }, [table.name]);

  useEffect(() => {
    setIdentifierValue(table.identifier);
  }, [table.identifier]);

  useEffect(() => {
    if (!isEditingName) {
      return;
    }

    const timer = window.setTimeout(() => {
      nameInputRef.current?.focus();
      nameInputRef.current?.select();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [isEditingName]);

  useEffect(() => {
    if (!isEditingIdentifier) {
      return;
    }

    const timer = window.setTimeout(() => {
      identifierInputRef.current?.focus();
      identifierInputRef.current?.select();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [isEditingIdentifier]);

  useEffect(() => {
    if (!isEditingDisabled) {
      return;
    }
    setIsEditingName(false);
    setIsEditingIdentifier(false);
    setNameValue(table.name);
    setIdentifierValue(table.identifier);
  }, [isEditingDisabled, table.identifier, table.name]);

  const handleNameChange = useCallback(
    (value: string) => {
      setNameValue(value);
      const trimmed = value.trim();
      if (trimmed) {
        onUpdateName(table.id, trimmed);
      }
    },
    [onUpdateName, table.id],
  );

  const handleIdentifierChange = useCallback(
    (value: string) => {
      setIdentifierValue(value);
      const trimmed = value.trim();
      onUpdateIdentifier(table.id, trimmed);
    },
    [onUpdateIdentifier, table.id],
  );

  const handleNameKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLInputElement>) => {
      if (event.key === 'Enter' || event.key === 'Escape') {
        event.preventDefault();
        // 엔터/ESC 키로 즉시 적용하지 않음 - 적용/취소 버튼으로만 제어
      }
    },
    [],
  );

  const handleIdentifierKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLInputElement>) => {
      if (event.key === 'Enter' || event.key === 'Escape') {
        event.preventDefault();
        // 엔터/ESC 키로 즉시 적용하지 않음 - 적용/취소 버튼으로만 제어
      }
    },
    [],
  );

  return (
    <div
      className={`flex items-center justify-between rounded-t-lg px-5 py-3 ${headerBorderClass} ${headerBgClass} ${headerTextClass}`}
      onMouseDown={onHeaderMouseDown}
    >
      {isEditingName ? (
        <input
          ref={nameInputRef}
          value={nameValue}
          onChange={(event) => handleNameChange(event.target.value)}
          onKeyDown={handleNameKeyDown}
          className="w-full max-w-[200px] rounded border border-[#2f4c92] bg-white px-2 py-1 text-base font-semibold text-[#1c2a4a] outline-none focus:border-blue focus:ring-1 focus:ring-blue"
        />
      ) : (
        <button
          type="button"
          onDoubleClick={() => {
            if (isEditingDisabled) {
              return;
            }
            setIsEditingIdentifier(false);
            setIsEditingName(true);
          }}
          className={`text-left text-base font-semibold focus:outline-none ${headerTextClass}`}
        >
          {table.name}
        </button>
      )}
      <div
        className={`flex items-center gap-2 text-sm tracking-wide ${subTextClass}`}
      >
        {isEditingIdentifier ? (
          <input
            ref={identifierInputRef}
            value={identifierValue}
            onChange={(event) => handleIdentifierChange(event.target.value)}
            onKeyDown={handleIdentifierKeyDown}
            className="w-full max-w-[140px] rounded border border-[#2f4c92] bg-white px-2 py-1 text-xs font-semibold tracking-wide text-[#1c2a4a] outline-none focus:border-blue focus:ring-1 focus:ring-blue"
          />
        ) : (
          <button
            type="button"
            onDoubleClick={() => {
              if (isEditingDisabled) {
                return;
              }
              setIsEditingName(false);
              setIsEditingIdentifier(true);
            }}
            className={`text-left font-semibold tracking-wide focus:outline-none ${subTextClass}`}
          >
            {table.identifier}
          </button>
        )}
      </div>
    </div>
  );
};

export default React.memo(TableCardHeader);
