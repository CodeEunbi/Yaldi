// types/api.ts (새 파일 생성 또는 기존 types 파일에 추가)

import type { AxiosError } from 'axios';

// API 에러 응답 타입
export interface ApiErrorResponse {
  message: string;
  code?: string;
  statusCode?: number;
}

// Axios 에러 타입
export type ApiError = AxiosError<ApiErrorResponse>;

// export interface ApiResponseData {
//   isSuccess: boolean;
//   code: string;
//   message: string;
//   result?: any;
// }