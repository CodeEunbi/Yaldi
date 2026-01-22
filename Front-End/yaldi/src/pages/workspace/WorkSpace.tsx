/* eslint-disable react-refresh/only-export-components */
import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type Dispatch,
  type SetStateAction,
} from 'react';
import { toPng } from 'html-to-image';
import { useAuthStore } from '../../stores/authStore';
import { DEFAULT_NOTE_COLOR, TABLE_COLOR_HEX_MAP } from './constants/colorOptions';
import { createTable as createTableApi } from '../../apis/tableApi';

export type SidebarTool =
  | 'cursor'
  | 'multi-select'
  | 'hand'
  | 'add-table'
  | 'memo'
  | '013-relation'
  | '01-relation-1'
  | '01-relation'
  | '13-relation'
  | '11-relation'
  | '1-relation'
  | 'import'
  | 'export';

export type WorkspaceTableColumn = {
  id: string;
  key?: number; // 백엔드 columnKey
  isKey: boolean;
  logicalName: string;
  physicalName: string;
  domain: string;
  dataType: string;
  allowNull: string;
  defaultValue: string;
  comment: string;
};

export type WorkspaceTableColor =
  | 'blue'
  | 'user2'
  | 'user3'
  | 'user4'
  | 'user5'
  | 'user6'
  | 'user7'
  | 'user8';

export type WorkspaceNote = {
  id: string;
  x: number;
  y: number;
  content: string;
  color: WorkspaceTableColor;
};

export type RelationType = 'identifying' | 'non-identifying';

export type WorkspaceRelation = {
  id: string;
  sourceTableId: string;
  targetTableId: string;
  type: RelationType;
  cardinality: '1' | '11' | '13' | '01' | '01-1' | '013';
};

export type WorkspaceTable = {
  id: string;
  key?: number; // 백엔드 tableKey
  name: string;
  identifier: string;
  x: number;
  y: number;
  color: WorkspaceTableColor;
  columns: WorkspaceTableColumn[];
};

export type WorkspaceSnapshot = {
  id: string;
  name: string;
  createdAt: number;
  tables: WorkspaceTable[];
  notes: WorkspaceNote[];
  relations: WorkspaceRelation[];
  pan: { x: number; y: number };
  zoom: number;
  previewDataUrl: string | null;
  createdBy: {
    id: string;
    name: string;
    avatarColor?: string;
  } | null;
};

interface WorkspaceContextValue {
  activeTool: SidebarTool;
  setActiveTool: (tool: SidebarTool) => void;
  tables: WorkspaceTable[];
  zoom: number;
  setZoom: Dispatch<SetStateAction<number>>;
  pan: { x: number; y: number };
  setPan: Dispatch<SetStateAction<{ x: number; y: number }>>;
  replaceTables: (
    tables: WorkspaceTable[],
    options?: { resetView?: boolean },
  ) => void;
  createTable: (name: string) => void;
  duplicateTable: (tableId: string) => void;
  updateTableName: (tableId: string, name: string) => void;
  updateTableIdentifier: (tableId: string, identifier: string) => void;
  updateTableColor: (tableId: string, color: WorkspaceTableColor) => void;
  addKeyColumn: (tableId: string) => void;
  addColumn: (tableId: string) => void;
  updateTableColumn: (
    tableId: string,
    columnId: string,
    updates: Partial<WorkspaceTableColumn>,
  ) => void;
  reorderTableColumn: (
    tableId: string,
    columnId: string,
    targetIndex: number,
  ) => void;
  deleteTableColumn: (tableId: string, columnId: string) => void;
  updateTablePosition: (
    tableId: string,
    position: { x: number; y: number },
  ) => void;
  deleteTable: (tableId: string) => void;
  notes: WorkspaceNote[];
  createNote: (position: { x: number; y: number }) => WorkspaceNote;
  updateNote: (noteId: string, updates: Partial<WorkspaceNote>) => void;
  deleteNote: (noteId: string) => void;
  relations: WorkspaceRelation[];
  createRelation: (
    sourceTableId: string,
    targetTableId: string,
    type: RelationType,
    cardinality: WorkspaceRelation['cardinality'],
  ) => void;
  deleteRelation: (relationId: string) => void;
  snapshots: WorkspaceSnapshot[];
  createSnapshot: (name?: string) => WorkspaceSnapshot | null;
  restoreSnapshot: (snapshotId: string) => void;
  deleteSnapshot: (snapshotId: string) => void;
  registerWorkspaceCanvas: (element: HTMLDivElement | null) => void;
}

const WorkspaceContext = createContext<WorkspaceContextValue | undefined>(
  undefined,
);

const DEFAULT_IDENTIFIER = 'Untitled';

const cloneTables = (tables: WorkspaceTable[]): WorkspaceTable[] =>
  tables.map((table) => ({
    ...table,
    columns: table.columns.map((column) => ({ ...column })),
  }));

const cloneNotes = (notes: WorkspaceNote[]): WorkspaceNote[] =>
  notes.map((note) => ({
    ...note,
    color: note.color ?? DEFAULT_NOTE_COLOR,
  }));

export const WorkspaceProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [activeTool, setActiveToolState] = useState<SidebarTool>('cursor');
  const [tables, setTables] = useState<WorkspaceTable[]>([]);
  const [notes, setNotes] = useState<WorkspaceNote[]>([]);
  const [relations, setRelations] = useState<WorkspaceRelation[]>([]);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [snapshots, setSnapshots] = useState<WorkspaceSnapshot[]>([]);
  const [workspaceCanvas, setWorkspaceCanvas] = useState<HTMLDivElement | null>(
    null,
  );
  const currentUser = useAuthStore((state) => state.currentUser);

  const setActiveTool = useCallback((tool: SidebarTool) => {
    setActiveToolState(tool);
  }, []);

  const replaceTables = useCallback(
    (
      nextTables: WorkspaceTable[],
      options: { resetView?: boolean } = { resetView: true },
    ) => {
      const shouldReset = options.resetView ?? true;
      setTables(cloneTables(nextTables));
      if (shouldReset) {
        setNotes([]);
      }
      if (shouldReset) {
        setPan({ x: 0, y: 0 });
        setZoom(1);
      }
    },
    [],
  );

  const createTable = useCallback(async (name: string) => {
    const trimmed = name.trim();
    if (!trimmed) {
      return;
    }

    // 기본 색상과 위치 계산
    const color: WorkspaceTableColor = 'blue';
    const xPosition = 200;
    const yPosition = 140;

    // 로컬에서 임시 테이블 생성 (즉시 UI 업데이트)
    const tempId = `table-${Date.now()}`;
    setTables((prev) => [
      ...prev,
      {
        id: tempId,
        name: trimmed,
        identifier: DEFAULT_IDENTIFIER,
        x: xPosition,
        y: yPosition,
        color,
        columns: [],
      },
    ]);

    // API 호출하여 서버에 테이블 생성
    try {
      const projectKey = useAuthStore.getState().projectKey;
      if (!projectKey) {
        console.error('프로젝트 키가 없습니다.');
        return;
      }

      const response = await createTableApi({
        projectKey,
        logicalName: trimmed,
        physicalName: trimmed,
        xPosition,
        yPosition,
        colorHex: TABLE_COLOR_HEX_MAP[color].replace('#', '').toLowerCase(),
      });

      // API 응답으로 받은 tableKey로 로컬 테이블 업데이트
      setTables((prev) =>
        prev.map((table) =>
          table.id === tempId
            ? { ...table, key: response.tableKey }
            : table,
        ),
      );
    } catch (error) {
      console.error('테이블 생성 실패:', error);
      // 실패 시 로컬에서 생성한 테이블 제거
      setTables((prev) => prev.filter((table) => table.id !== tempId));
    }
  }, []);

  const duplicateTable = useCallback((tableId: string) => {
    setTables((prev) => {
      const source = prev.find((table) => table.id === tableId);
      if (!source) {
        return prev;
      }

      const timestamp = Date.now();
      const newTableId = `table-${timestamp}-${Math.random()}`;
      const duplicatedColumns = source.columns.map((column, index) => ({
        ...column,
        id: `column-${timestamp}-${index}-${Math.random()}`,
      }));

      return [
        ...prev,
        {
          ...source,
          id: newTableId,
          name: `Copy of ${source.name}`,
          x: source.x + 32,
          y: source.y + 32,
          columns: duplicatedColumns,
        },
      ];
    });
  }, []);

  const updateTableName = useCallback((tableId: string, name: string) => {
    const trimmed = name.trim();
    if (!trimmed) {
      return;
    }

    setTables((prev) =>
      prev.map((table) =>
        table.id === tableId
          ? {
              ...table,
              name: trimmed,
            }
          : table,
      ),
    );
  }, []);

  const updateTableIdentifier = useCallback(
    (tableId: string, identifier: string) => {
      const trimmed = identifier.trim() || DEFAULT_IDENTIFIER;

      setTables((prev) =>
        prev.map((table) =>
          table.id === tableId
            ? {
                ...table,
                identifier: trimmed,
              }
            : table,
        ),
      );
    },
    [],
  );

  const addKeyColumn = useCallback((tableId: string) => {
    setTables((prev) =>
      prev.map((table) =>
        table.id === tableId
          ? {
              ...table,
              columns: [
                ...table.columns,
                {
                  id: `column-${Date.now()}`,
                  isKey: true,
                  logicalName: 'Logical Name(key)',
                  physicalName: 'Physical Name(key)',
                  domain: 'Domain',
                  dataType: 'Type',
                  allowNull: 'Allow null',
                  defaultValue: 'Default value',
                  comment: 'Comment',
                },
              ],
            }
          : table,
      ),
    );
  }, []);

  const updateTableColumn = useCallback(
    (
      tableId: string,
      columnId: string,
      updates: Partial<WorkspaceTableColumn>,
    ) => {
      setTables((prev) =>
        prev.map((table) =>
          table.id === tableId
            ? {
                ...table,
                columns: table.columns.map((column) =>
                  column.id === columnId
                    ? {
                        ...column,
                        ...updates,
                      }
                    : column,
                ),
              }
            : table,
        ),
      );
    },
    [],
  );

  const addColumn = useCallback((tableId: string) => {
    setTables((prev) =>
      prev.map((table) =>
        table.id === tableId
          ? {
              ...table,
              columns: [
                ...table.columns,
                {
                  id: `column-${Date.now() + Math.random()}`,
                  isKey: false,
                  logicalName: 'Logical Name',
                  physicalName: 'Physical Name',
                  domain: 'Domain',
                  dataType: 'Type',
                  allowNull: 'Allow null',
                  defaultValue: 'Default value',
                  comment: 'Comment',
                },
              ],
            }
          : table,
      ),
    );
  }, []);

  const updateTableColor = useCallback(
    (tableId: string, color: WorkspaceTableColor) => {
      setTables((prev) =>
        prev.map((table) =>
          table.id === tableId
            ? {
                ...table,
                color,
              }
            : table,
        ),
      );
    },
    [],
  );

  const reorderTableColumn = useCallback(
    (tableId: string, columnId: string, targetIndex: number) => {
      setTables((prev) =>
        prev.map((table) => {
          if (table.id !== tableId) {
            return table;
          }

          const currentIndex = table.columns.findIndex(
            (column) => column.id === columnId,
          );
          if (currentIndex === -1 || table.columns.length <= 1) {
            return table;
          }

          const nextColumns = [...table.columns];
          const [movingColumn] = nextColumns.splice(currentIndex, 1);
          if (!movingColumn) {
            return table;
          }

          const clampedTarget = Math.max(
            0,
            Math.min(targetIndex, nextColumns.length),
          );
          nextColumns.splice(clampedTarget, 0, movingColumn);

          return {
            ...table,
            columns: nextColumns,
          };
        }),
      );
    },
    [],
  );

  const deleteTableColumn = useCallback((tableId: string, columnId: string) => {
    setTables((prev) =>
      prev.map((table) =>
        table.id === tableId
          ? {
              ...table,
              columns: table.columns.filter((column) => column.id !== columnId),
            }
          : table,
      ),
    );
  }, []);

  const updateTablePosition = useCallback(
    (tableId: string, position: { x: number; y: number }) => {
      setTables((prev) =>
        prev.map((table) =>
          table.id === tableId
            ? {
                ...table,
                x: position.x,
                y: position.y,
              }
            : table,
        ),
      );
    },
    [],
  );

  const deleteTable = useCallback((tableId: string) => {
    setTables((prev) => prev.filter((table) => table.id !== tableId));
  }, []);

  const createNote = useCallback((position: { x: number; y: number }) => {
    const note: WorkspaceNote = {
      id: `note-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      x: position.x,
      y: position.y,
      content: '',
      color: DEFAULT_NOTE_COLOR,
    };
    setNotes((prev) => [...prev, note]);
    return note;
  }, []);

  const updateNote = useCallback(
    (noteId: string, updates: Partial<WorkspaceNote>) => {
      setNotes((prev) =>
        prev.map((note) =>
          note.id === noteId
            ? {
                ...note,
                ...updates,
              }
            : note,
        ),
      );
    },
    [],
  );

  const deleteNote = useCallback((noteId: string) => {
    setNotes((prev) => prev.filter((note) => note.id !== noteId));
  }, []);

  const createRelation = useCallback(
    (
      sourceTableId: string,
      targetTableId: string,
      type: RelationType,
      cardinality: WorkspaceRelation['cardinality'],
    ) => {
      const newRelation: WorkspaceRelation = {
        id: `relation-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
        sourceTableId,
        targetTableId,
        type,
        cardinality,
      };
      setRelations((prev) => [...prev, newRelation]);
    },
    [],
  );

  const deleteRelation = useCallback((relationId: string) => {
    setRelations((prev) => prev.filter((relation) => relation.id !== relationId));
  }, []);

  const createSnapshot = useCallback(
    (name?: string) => {
      const trimmedName = name?.trim();
      const snapshotId = `snapshot-${Date.now()}-${Math.random()
        .toString(36)
        .slice(2, 10)}`;
      const snapshot: WorkspaceSnapshot = {
        id: snapshotId,
        name:
          trimmedName && trimmedName.length > 0
            ? trimmedName
            : `스냅샷 ${snapshots.length + 1}`,
        createdAt: Date.now(),
        tables: cloneTables(tables),
        notes: cloneNotes(notes),
        relations: relations.map((relation) => ({ ...relation })),
        pan: { ...pan },
        zoom,
        previewDataUrl: null,
        createdBy: currentUser
          ? {
              id: currentUser.userKey.toString(),
              name: currentUser.nickname,
              avatarColor: currentUser.avatarColor,
            }
          : null,
      };
      setSnapshots((prev) => [snapshot, ...prev]);
      const canvasElement = workspaceCanvas;
      if (canvasElement) {
        window.setTimeout(() => {
          toPng(canvasElement, {
            cacheBust: true,
            pixelRatio: window.devicePixelRatio ?? 1,
          })
            .then((dataUrl: string) => {
              setSnapshots((prev: WorkspaceSnapshot[]) => {
                const typedPrev: WorkspaceSnapshot[] = prev;
                for (let index = 0; index < typedPrev.length; index += 1) {
                  const item: WorkspaceSnapshot = typedPrev[index];
                  if (item.id === snapshotId) {
                    const next = [...typedPrev];
                    next[index] = {
                      ...item,
                      previewDataUrl: dataUrl,
                    };
                    return next;
                  }
                }
                return typedPrev;
              });
            })
            .catch((error: unknown) => {
              console.error('스냅샷 미리보기 생성 실패', error);
            });
        }, 0);
      }
      return snapshot;
    },
    [currentUser, notes, pan, snapshots.length, tables, workspaceCanvas, zoom],
  );

  const restoreSnapshot = useCallback(
    (snapshotId: string) => {
      const snapshot = snapshots.find((item) => item.id === snapshotId);
      if (!snapshot) {
        return;
      }
      setTables(cloneTables(snapshot.tables));
      setNotes(cloneNotes(snapshot.notes));
      setRelations(snapshot.relations.map((relation) => ({ ...relation })));
      setPan({ ...snapshot.pan });
      setZoom(snapshot.zoom);
    },
    [snapshots],
  );

  const deleteSnapshot = useCallback((snapshotId: string) => {
    setSnapshots((prev) =>
      prev.filter((snapshot) => snapshot.id !== snapshotId),
    );
  }, []);

  const registerWorkspaceCanvas = useCallback(
    (element: HTMLDivElement | null) => {
      setWorkspaceCanvas(element);
    },
    [],
  );

  const value = useMemo(
    () => ({
      activeTool,
      setActiveTool,
      tables,
      zoom,
      setZoom,
      pan,
      setPan,
      replaceTables,
      createTable,
      duplicateTable,
      updateTableName,
      updateTableIdentifier,
      updateTableColor,
      addKeyColumn,
      addColumn,
      updateTableColumn,
      reorderTableColumn,
      deleteTableColumn,
      updateTablePosition,
      deleteTable,
      notes,
      createNote,
      updateNote,
      deleteNote,
      relations,
      createRelation,
      deleteRelation,
      snapshots,
      createSnapshot,
      restoreSnapshot,
      deleteSnapshot,
      registerWorkspaceCanvas,
    }),
    [
      activeTool,
      setActiveTool,
      tables,
      zoom,
      setZoom,
      pan,
      setPan,
      replaceTables,
      createTable,
      duplicateTable,
      updateTableName,
      updateTableIdentifier,
      updateTableColor,
      addKeyColumn,
      addColumn,
      updateTableColumn,
      reorderTableColumn,
      deleteTableColumn,
      updateTablePosition,
      deleteTable,
      notes,
      createNote,
      updateNote,
      deleteNote,
      relations,
      createRelation,
      deleteRelation,
      snapshots,
      createSnapshot,
      restoreSnapshot,
      deleteSnapshot,
      registerWorkspaceCanvas,
    ],
  );

  return (
    <WorkspaceContext.Provider value={value}>
      {children}
    </WorkspaceContext.Provider>
  );
};

export const useWorkspace = (): WorkspaceContextValue => {
  const context = useContext(WorkspaceContext);
  if (!context) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }
  return context;
};
