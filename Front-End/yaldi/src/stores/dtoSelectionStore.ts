import { create } from 'zustand';

export interface SelectedColumn {
  tableId: string;
  columnId: string;
}

interface DtoSelectionStore {
  isSelecting: boolean;
  selectedColumns: Record<string, SelectedColumn>;
  startSelection: () => void;
  cancelSelection: () => void;
  toggleColumn: (tableId: string, columnId: string) => void;
  setColumnSelection: (
    tableId: string,
    columnId: string,
    selected: boolean,
  ) => void;
  clearSelection: () => void;
}

const makeKey = (tableId: string, columnId: string) => `${tableId}:${columnId}`;

export const useDtoSelectionStore = create<DtoSelectionStore>((set) => ({
  isSelecting: false,
  selectedColumns: {},
  startSelection: () => set({ isSelecting: true, selectedColumns: {} }),
  cancelSelection: () => set({ isSelecting: false, selectedColumns: {} }),
  toggleColumn: (tableId, columnId) =>
    set((state) => {
      const key = makeKey(tableId, columnId);
      const nextSelected = { ...state.selectedColumns };

      if (nextSelected[key]) {
        delete nextSelected[key];
      } else {
        nextSelected[key] = { tableId, columnId };
      }

      return { selectedColumns: nextSelected };
    }),
  setColumnSelection: (tableId, columnId, selected) =>
    set((state) => {
      const key = makeKey(tableId, columnId);
      const nextSelected = { ...state.selectedColumns };

      if (selected) {
        nextSelected[key] = { tableId, columnId };
      } else {
        delete nextSelected[key];
      }

      return { selectedColumns: nextSelected };
    }),
  clearSelection: () => set({ selectedColumns: {} }),
}));
