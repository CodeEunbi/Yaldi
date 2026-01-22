# 📁 types

## TypeScript 타입 정의

> 인터페이스, 타입 별칭, API 응답 타입 등

```ts
// user.ts
export interface User {
  id: string;
  name: string;
  email: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
}

// api.ts
export interface ApiResponse<T = unknown> {
  isSuccess: boolean;
  code: string;
  message: string;
  result: T;
}
```
