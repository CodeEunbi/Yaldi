import React from "react";
import { fetchSearchResults } from "../apis/searchApi";
import type { Project } from "../types/search";

// - useSearchResults(query: string | null)
// - searchApi 호출
// - loading, error, data 상태 관리
// - 검색어 변경 시 자동 재검색

export const useSearchResults = (query: string) => {

  const [data, setData] = React.useState<Project[] | null>(null);
  const [isLoading, setIsLoading] = React.useState<boolean>(false);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    const loadResults = async () => {
      try {
        setIsLoading(true);
        setError(null);
        const results: Project[] = await fetchSearchResults(query);
        console.log("새 데이터 불러오기 완료");
        setData(results);
      } catch (err) {
        console.log("Err:", err);
        setError("검색 결과를 불러오는 데 실패했습니다.");
      } finally {
        setIsLoading(false);
      }
    };

    if (query && query.trim() !== "") {
      loadResults();
    } else {
      setData(null);
    }
      

  }, [query]);

  return {
    isLoading,
    data,
    error
  } as const;
};