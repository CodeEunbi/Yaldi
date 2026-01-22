import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface User {
  userKey: number;
  email?: string;
  nickname: string;
  createdAt?: string;
  updatedAt?: string;
  avatarColor?: string;
}

interface AuthStore {
  isLoggedIn: boolean;
  currentUser: User | null;
  projectName: string;
  projectKey: number | null;
  currentMode: 'ver' | 'erd' | 'datamodel';
  // 임시: 협업자 목록 (나중에 API로 교체)
  collaborators: User[];
  // Actions
  setProjectName: (name: string) => void;
  setProjectKey: (key: number | null) => void;
  setCurrentMode: (mode: 'ver' | 'erd' | 'datamodel') => void;
  login: (user: User) => void;
  logout: () => void;
  // 나중에 API 연동 시 이 부분만 수정하면 됨
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      // 임시 데이터
      isLoggedIn: false,
      currentUser: null,
      projectName: '',
      projectKey: null,
      currentMode: 'erd',
      collaborators: [],
      // projectName: 'Yaldi 프로젝트',
      // currentMode: 'erd',
      // collaborators: [
      //   { userKey: 1, nickname: '김은비', avatarColor: 'user2' },
      //   { userKey: 2, nickname: '연지윤', avatarColor: 'user3' },
      //   { userKey: 3, nickname: '김서원', avatarColor: 'user4' },
      // ],
      // Actions
      setProjectName: (name) => set({ projectName: name }),
      setProjectKey: (key) => set({ projectKey: key }),
      setCurrentMode: (mode) => set({ currentMode: mode }),
      login: (user) => set({ isLoggedIn: true, currentUser: user }),
      logout: () => set({ isLoggedIn: false, currentUser: null }),
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => sessionStorage),
    }
  )
);
