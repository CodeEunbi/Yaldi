import type { CreateTableRequest, CreateTableResponse } from '../types/table';
import { apiController } from './apiController';

/**
 * 테이블 생성 API
 * @param data 테이블 생성 요청 데이터
 * @returns 생성된 테이블 정보
 */
export const createTable = async (
  data: CreateTableRequest,
): Promise<CreateTableResponse> => {
  try {
    const { projectKey, ...requestBody } = data;

    const response = await apiController({
      url: `/api/v1/erd/projects/${projectKey}/tables`,
      method: 'post',
      data: requestBody,
    });

    console.log('테이블 생성 성공:', response.data.result);
    return response.data.result;
  } catch (error) {
    console.error('테이블 생성 API 호출 실패:', error);
    throw error;
  }
};