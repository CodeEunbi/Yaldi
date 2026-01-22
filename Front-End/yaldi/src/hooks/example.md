# 📁 hooks

## 커스텀 React 훅

> 재사용 가능한 상태 로직, 비즈니스 로직을 캡슐화한 커스텀 훅

```ts
// useAuth.ts
import { useState } from "react";

// 인증 상태 관리
export function useAuth() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  const login = () => {
    // 로그인
    setIsLoggedIn(true);
  };

  const logout = () => {
    // 로그아웃
    setIsLoggedIn(false);
  };

  return { isLoggedIn, login, logout };
}
```

예시일 뿐 사용은 안해요~
