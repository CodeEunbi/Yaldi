# 📁 apis

## API 통신 관련 코드

> Axios 클라이언트, API 호출 함수, HTTP 요청/응답 처리 로직 담당

```ts
// auth.ts
import apiController from './apiController';

interface LoginPayload {
  email: string;
  password: string;
}

// Send login request to server
export async function loginUser(payload: LoginPayload) {
  // 1
  const config = {
    method: 'GET' as const,
    url: '/url',
    params: {
      key: value,
      ...
    }
  };
  const response = await apiController(config);

  // 2
  const response = await apiController({
    method: 'POST' as const,
    url: '/url',
    data: {
      body: body,
      ...
    }
  });

  return response.data.result; // 백에서 받아오는 데이터 위치
}
```

📍 ResponseDTO → `response.data`에 아래 객체가 들어갑니다

```json
{
  "isSuccess": true,
  "code": "string", // 임의 지정 코드
  "message": "string", // 에러 메시지
  "result": {
    // 실제 데이터
    "id": 0,
    "email": "string",
    "name": "string",
    "nickname": "string"
  }
}
```
