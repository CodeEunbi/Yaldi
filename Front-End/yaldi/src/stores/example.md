# 📁 stores

## 상태 관리 스토어

> Zustand, Redux Toolkit, Jotai 등 상태 관리 라이브러리 스토어

```ts
// userStore.ts (Zustand 예시)
import { create } from "zustand";

interface UserStore {
  users: User[];
  currentUser: User | null;
  setUsers: (users: User[]) => void;
  setCurrentUser: (user: User | null) => void;
  addUser: (user: User) => void;
}

export const useUserStore = create<UserStore>((set) => ({
  users: [],
  currentUser: null,
  setUsers: (users) => set({ users }),
  setCurrentUser: (currentUser) => set({ currentUser }),
  addUser: (user) => set((state) => ({ users: [...state.users, user] })),
}));
```

📍 플젝 규모가 막 크지 않으니 zustand 사용합시다
