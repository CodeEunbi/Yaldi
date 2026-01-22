import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useDtoSelectionStore } from '../../../stores/dtoSelectionStore';
import { useEntitySelectionStore } from '../../../stores/entitySelectionStore';
import TableCardHeader from './TableCardHeader';
import TableCardBody from './TableCardBody';
import TableCardActionButtons from './TableCardActionButtons';
import TableCardCommentPanel from './TableCardCommentPanel';
import TableCardFooter from './TableCardFooter';
import type {
  WorkspaceTable,
  TableActionButton,
  TableComment,
} from '../../../types/tableCard';

type TableCardProps = {
  table: WorkspaceTable;
  isSelected: boolean;
  onSelect: (tableId: string) => void;
  actions: TableActionButton[];
  isEditingDisabled?: boolean;
  onUpdateName: (tableId: string, name: string) => void;
  onUpdateIdentifier: (tableId: string, identifier: string) => void;
  onUpdateColumn: (
    tableId: string,
    columnId: string,
    updates: Partial<WorkspaceTable['columns'][number]>,
  ) => void;
  onReorderColumn: (
    tableId: string,
    columnId: string,
    targetIndex: number,
  ) => void;
  onDeleteColumn: (tableId: string, columnId: string) => void;
  onUpdateColor: (tableId: string, color: WorkspaceTable['color']) => void;
  onDragStart?: (
    event: React.MouseEvent<HTMLDivElement>,
    table: WorkspaceTable,
    bounds: DOMRect | null,
  ) => void;
  comments?: TableComment[];
  isCommentOpen?: boolean;
  onToggleComments?: (tableId: string) => void;
  onCloseComments?: () => void;
  onSubmitComment?: (tableId: string, content: string) => void;
  onUpdateComment?: (
    tableId: string,
    commentId: string,
    content: string,
  ) => void;
  onDeleteComment?: (tableId: string, commentId: string) => void;
  onSubmitReply?: (tableId: string, commentId: string, content: string) => void;
  onUpdateReply?: (
    tableId: string,
    commentId: string,
    replyId: string,
    content: string,
  ) => void;
  onDeleteReply?: (tableId: string, commentId: string, replyId: string) => void;
  isCommentResolved?: boolean;
  onToggleCommentResolved?: (tableId: string) => void;
};

const TableCard: React.FC<TableCardProps> = ({
  table,
  isSelected,
  onSelect,
  actions,
  isEditingDisabled = false,
  onUpdateName,
  onUpdateIdentifier,
  onUpdateColumn,
  onReorderColumn,
  onDeleteColumn,
  onUpdateColor,
  onDragStart,
  comments = [],
  isCommentOpen = false,
  onToggleComments,
  onCloseComments,
  onSubmitComment,
  onUpdateComment,
  onDeleteComment,
  onSubmitReply,
  onUpdateReply,
  onDeleteReply,
  isCommentResolved = false,
  onToggleCommentResolved,
}) => {
  const isDtoSelecting = useDtoSelectionStore((state) => state.isSelecting);
  const selectedDtoColumns = useDtoSelectionStore(
    (state) => state.selectedColumns,
  );
  const setColumnSelection = useDtoSelectionStore(
    (state) => state.setColumnSelection,
  );

  const isEntitySelecting = useEntitySelectionStore(
    (state) => state.isSelecting,
  );
  const selectedEntityTableId = useEntitySelectionStore(
    (state) => state.selectedTableId,
  );
  const setSelectedEntityTable = useEntitySelectionStore(
    (state) => state.setSelectedTable,
  );

  const cardRef = useRef<HTMLDivElement | null>(null);
  const memoButtonRef = useRef<HTMLButtonElement | null>(null);

  const [isEditing, setIsEditing] = useState(false);
  const [originalTable, setOriginalTable] = useState<WorkspaceTable | null>(null);

  const memoAction = useMemo(
    () => actions.find((action) => action.key === 'create-memo'),
    [actions],
  );

  useEffect(() => {
    if (!isSelected && isCommentOpen) {
      onCloseComments?.();
    }
  }, [isCommentOpen, isSelected, onCloseComments]);

  // 편집 중일 때 선택 해제 방지
  useEffect(() => {
    if (!isSelected && isEditing) {
      // 편집 중인데 선택이 해제되면 다시 선택
      onSelect(table.id);
    }
  }, [isSelected, isEditing, onSelect, table.id]);

  const handleSelect = useCallback(
    (event: React.MouseEvent<HTMLDivElement>) => {
      event.stopPropagation();
      onSelect(table.id);
      if (!isEditing) {
        setIsEditing(true);
        setOriginalTable(table);
      }
    },
    [onSelect, table, isEditing],
  );

  const handleApply = useCallback(() => {
    setIsEditing(false);
    setOriginalTable(null);
    onSelect(''); // 테이블 선택 해제
    // TODO: 웹소켓으로 unlock 메시지 전송
  }, [onSelect]);

  const handleCancel = useCallback(() => {
    if (originalTable) {
      // 원래 상태로 복구
      onUpdateName(table.id, originalTable.name);
      onUpdateIdentifier(table.id, originalTable.identifier);
      // 컬럼도 원래대로 복구 필요시 추가
    }
    setIsEditing(false);
    setOriginalTable(null);
    onSelect(''); // 테이블 선택 해제
    // TODO: 웹소켓으로 unlock 메시지 전송
  }, [originalTable, table.id, onUpdateName, onUpdateIdentifier, onSelect]);

  const handleHeaderMouseDown = useCallback(
    (event: React.MouseEvent<HTMLDivElement>) => {
      if (event.button !== 0) {
        return;
      }
      const target = event.target as HTMLElement;
      if (target.closest('button, input, textarea')) {
        return;
      }
      onDragStart?.(
        event,
        table,
        cardRef.current?.getBoundingClientRect() ?? null,
      );
    },
    [onDragStart, table],
  );

  const outerBorderClass = isSelected
    ? 'border-2 border-[#1f3f9d]'
    : 'border-[0.5px] border-[#9fb4ec]';
  const headerBorderClass = isSelected
    ? 'border-b-[0.5px] border-[#1f3f9d]'
    : 'border-b-[0.5px] border-[#9fb4ec]';
  const lightBackgroundColors: WorkspaceTable['color'][] = [
    'user3',
    'user5',
    'user6',
    'user7',
  ];
  const isLightBackground = lightBackgroundColors.includes(table.color);
  const headerBgClass = `bg-${table.color}`;
  const headerTextClass = isLightBackground ? 'text-[#1c2a4a]' : 'text-white';
  const subTextClass = isLightBackground ? 'text-[#1c2a4a]' : 'text-white/80';

  return (
    <div
      ref={cardRef}
      data-table-id={table.id}
      onClick={handleSelect}
      className={`absolute min-w-fit overflow-visible rounded-lg bg-white transition-all ${outerBorderClass}`}
      style={{ top: table.y, left: table.x }}
    >
      {isEntitySelecting ? (
        <label
          className="absolute -left-6 top-4 z-30 flex h-5 w-5 -translate-x-full items-center justify-center"
          onMouseDown={(event) => event.stopPropagation()}
          onClick={(event) => event.stopPropagation()}
        >
          <input
            type="checkbox"
            className="h-4 w-4 accent-blue"
            checked={selectedEntityTableId === table.id}
            onChange={(event) =>
              setSelectedEntityTable(event.target.checked ? table.id : null)
            }
            onClick={(event) => event.stopPropagation()}
          />
        </label>
      ) : null}
      <TableCardActionButtons
        tableId={table.id}
        tableColor={table.color}
        actions={actions}
        isSelected={isSelected}
        isEditingDisabled={isEditingDisabled}
        onUpdateColor={onUpdateColor}
      />
      <TableCardCommentPanel
        tableId={table.id}
        memoAction={memoAction}
        isSelected={isSelected}
        isEditingDisabled={isEditingDisabled}
        isCommentOpen={isCommentOpen}
        comments={comments}
        isCommentResolved={isCommentResolved}
        memoButtonRef={memoButtonRef}
        onToggleComments={onToggleComments}
        onCloseComments={onCloseComments}
        onToggleCommentResolved={onToggleCommentResolved}
        onSubmitComment={onSubmitComment}
        onUpdateComment={onUpdateComment}
        onDeleteComment={onDeleteComment}
        onSubmitReply={onSubmitReply}
        onUpdateReply={onUpdateReply}
        onDeleteReply={onDeleteReply}
      />
      <TableCardHeader
        table={table}
        isEditingDisabled={isEditingDisabled}
        headerBorderClass={headerBorderClass}
        headerBgClass={headerBgClass}
        headerTextClass={headerTextClass}
        subTextClass={subTextClass}
        onUpdateName={onUpdateName}
        onUpdateIdentifier={onUpdateIdentifier}
        onHeaderMouseDown={handleHeaderMouseDown}
      />
      <TableCardBody
        table={table}
        isSelected={isSelected}
        isEditingDisabled={isEditingDisabled}
        isDtoSelecting={isDtoSelecting}
        selectedDtoColumns={selectedDtoColumns}
        setColumnSelection={setColumnSelection}
        onUpdateColumn={onUpdateColumn}
        onReorderColumn={onReorderColumn}
        onDeleteColumn={onDeleteColumn}
      />
      <TableCardFooter
        isVisible={isSelected && isEditing}
        onApply={handleApply}
        onCancel={handleCancel}
      />
    </div>
  );
};

export type {
  TableActionButton,
  TableComment,
  TableReply,
} from '../../../types/tableCard';
export default TableCard;
