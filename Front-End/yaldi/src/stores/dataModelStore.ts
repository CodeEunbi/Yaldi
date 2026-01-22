import { create } from 'zustand';
import type { DataModelItem } from '../types/dataModel';

const initialDataModels: DataModelItem[] = [
  {
    dataModelKey: 101,
    name: 'UserAccount',
    type: 'entity',
    description: '사용자 인증 및 기본 프로필 정보를 저장합니다.',
    relatedEntities: ['UserSession', 'UserProfile', 'Team'],
    createAt: new Date('2025-05-01T10:00:00Z'),
    updatedAt: new Date('2025-11-01T12:00:00Z'),
    typeScript: `export interface UserProfile {
  id: number;
  username: string;
  email: string;
  isVerified: boolean;
  bio: string;
}`,
    java: `public class UserSummaryDto {
  private Long userId;
  private String nickname;
  private String profileImageUrl;
  private boolean hasUnreadNotifications;
}`,
  },
  {
    dataModelKey: 102,
    name: 'ProjectDiagram',
    type: 'entity',
    description:
      '캔버스에 배치된 모든 다이어그램 요소의 JSON 구조를 저장합니다.',
    relatedEntities: ['Project', 'DiagramSnapshot', 'UserAccount'],
    createAt: new Date('2025-06-15T15:30:00Z'),
    typeScript: `export interface ProjectDiagram {
  projectId: number;
  diagramJson: string;
  updatedAt: string;
}`,
    java: `public class ProjectDiagram {
  private Long projectId;
  private String diagramJson;
  private String updatedAt;
}`,
  },
  {
    dataModelKey: 103,
    name: 'NotificationLog',
    type: 'entity',
    description: '사용자에게 전송된 알림 기록 및 상태(읽음 여부)를 기록합니다.',
    relatedEntities: ['UserAccount', 'Project'],
    createAt: new Date('2025-10-20T08:45:00Z'),
    typeScript: `export interface NotificationLog {
  notificationId: number;
  userId: number;
  content: string;
  sentAt: string;
  isRead: boolean;
}`,
    java: `public class NotificationLog {
  private Long notificationId;
  private Long userId;
  private String content;
  private String sentAt;
  private Boolean isRead;
}`,
  },
];

interface DataModelStore {
  dataModels: DataModelItem[];
  addDataModels: (items: DataModelItem[]) => void;
  updateDataModel: (
    dataModelKey: number,
    updates: Partial<Omit<DataModelItem, 'dataModelKey'>>,
  ) => void;
  deleteDataModel: (dataModelKey: number) => void;
  getNextKey: () => number;
}

const generateNextKey = (items: DataModelItem[]): number => {
  const maxKey = items.reduce(
    (max, item) => (item.dataModelKey > max ? item.dataModelKey : max),
    0,
  );
  const timestampKey = Date.now();
  return Math.max(maxKey + 1, timestampKey);
};

export const useDataModelStore = create<DataModelStore>((set, get) => ({
  dataModels: initialDataModels,
  addDataModels: (items) =>
    set((state) => ({
      dataModels: [...items, ...state.dataModels],
    })),
  updateDataModel: (dataModelKey, updates) =>
    set((state) => ({
      dataModels: state.dataModels.map((item) =>
        item.dataModelKey === dataModelKey ? { ...item, ...updates } : item,
      ),
    })),
  deleteDataModel: (dataModelKey) =>
    set((state) => ({
      dataModels: state.dataModels.filter(
        (item) => item.dataModelKey !== dataModelKey,
      ),
    })),
  getNextKey: () => generateNextKey(get().dataModels),
}));
