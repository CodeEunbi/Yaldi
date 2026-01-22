// stores/notificationStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface NotificationStore {
  hasUnread: boolean;
  unreadCount: number;
  lastNotificationId: number | null;
  
  // 액션들
  setHasUnread: (hasUnread: boolean) => void;
  setUnreadCount: (count: number) => void;
  incrementUnread: () => void;
  markAllAsRead: () => void;
  setLastNotificationId: (id: number) => void;
}

export const useNotificationStore = create<NotificationStore>()(
  persist(
    (set) => ({
      hasUnread: false,
      unreadCount: 0,
      lastNotificationId: null,
      
      setHasUnread: (hasUnread) => set({ hasUnread }),
      
      setUnreadCount: (count) => set({ 
        unreadCount: count,
        hasUnread: count > 0 
      }),
      
      incrementUnread: () => set((state) => ({ 
        unreadCount: state.unreadCount + 1,
        hasUnread: true 
      })),
      
      markAllAsRead: () => set({ 
        hasUnread: false,
        unreadCount: 0 
      }),
      
      setLastNotificationId: (id) => set({ lastNotificationId: id }),
    }),
    {
      name: 'notification-storage', // localStorage 키
      partialize: (state) => ({
        // persist할 필드만 선택
        hasUnread: state.hasUnread,
        unreadCount: state.unreadCount,
        lastNotificationId: state.lastNotificationId,
      }),
    }
  )
);