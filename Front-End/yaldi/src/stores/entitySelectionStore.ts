import { create } from 'zustand';

interface EntitySelectionStore {
  isSelecting: boolean;
  selectedTableId: string | null;
  startSelection: () => void;
  cancelSelection: () => void;
  setSelectedTable: (tableId: string | null) => void;
}

export const useEntitySelectionStore = create<EntitySelectionStore>((set) => ({
  isSelecting: false,
  selectedTableId: null,
  startSelection: () => set({ isSelecting: true, selectedTableId: null }),
  cancelSelection: () => set({ isSelecting: false, selectedTableId: null }),
  setSelectedTable: (tableId) =>
    set((state) => ({
      selectedTableId: tableId,
      isSelecting: state.isSelecting,
    })),
}));
