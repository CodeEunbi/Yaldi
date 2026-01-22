import type { WorkspaceTableColor } from '../WorkSpace';

export const TABLE_COLOR_OPTIONS = [
  { key: 'myBlue', value: 'blue' as WorkspaceTableColor },
  { key: 'user2', value: 'user2' as WorkspaceTableColor },
  { key: 'user3', value: 'user3' as WorkspaceTableColor },
  { key: 'user4', value: 'user4' as WorkspaceTableColor },
  { key: 'user5', value: 'user5' as WorkspaceTableColor },
  { key: 'user6', value: 'user6' as WorkspaceTableColor },
  { key: 'user7', value: 'user7' as WorkspaceTableColor },
  { key: 'user8', value: 'user8' as WorkspaceTableColor },
] as const;

export const TABLE_COLOR_HEX_MAP: Record<WorkspaceTableColor, string> = {
  blue: '#1E50AF',
  user2: '#5271C4',
  user3: '#70B2FF',
  user4: '#44C2B3',
  user5: '#A3E0A9',
  user6: '#FFD166',
  user7: '#FF9F5A',
  user8: '#C18AFF',
} as const;

export const DEFAULT_NOTE_COLOR: WorkspaceTableColor = 'user6';
