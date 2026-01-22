// 프로젝트 정보 불러오는 훅.
// projectKey 가지고 `/api/v1/projects/{projectKey}` 으로 get 요청.

import React from "react";
import { apiController } from "../apis/apiController";
import type { Project } from "../types/search";

export const useGetProjectInfo = (projectKey: number) => {

  const [data, setData] = React.useState<Project | null>(null);
  const [isLoading, setIsLoading] = React.useState<boolean>(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    const loadResults = async () => {
      try {
        setIsLoading(true);
        setError(null);
        const response = await apiController({
          url: `/api/v1/projects/${encodeURIComponent(projectKey)}`,
          method: 'get',
        });
        console.log("새 데이터 불러오기 완료");
        setData(response.data.result);
      } catch (err) {
        console.log("Err:", err);
        setError("프로젝트 결과를 불러오는 데 실패했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    if (projectKey) {
      loadResults();
    } else {
      setData(null);
    }
  }, [projectKey]);

  return {
    isLoading,
    data,
    error
  } as const;
};
