import type { projectInfo } from "../types/project";
import { apiController } from "./apiController";

export const fetchSearchResults = async (query: string): Promise<projectInfo[]> => {
  if (!query) {
    return [];
  };

  try {

    const response = await apiController({
      url: `/api/v1/search/projects`,
      method: 'get',
      params: {
        query,
      }
    })

    console.log(query, "검색 성공", response.data.result);
    return response.data.result;

  } catch (error) {
    console.error("검색 API 호출 실패:", error);
    throw error;
  }
};