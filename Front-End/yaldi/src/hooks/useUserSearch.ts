// hooks/useUserSearch.ts

import { useState, useCallback } from 'react';
import { apiController } from '../apis/apiController';
import Swal from 'sweetalert2';

export interface SearchedUser {
  userKey: number;
  nickname: string;
  email: string;
  status: string;
}

interface UseUserSearchReturn {
  searchResults: SearchedUser[] | null;
  isSearching: boolean;
  searchError: string | null;
  searchUsers: (keyword: string, teamKey: number) => Promise<void>;
  clearSearch: () => void;
}

export const useUserSearch = (): UseUserSearchReturn => {
  const [searchResults, setSearchResults] = useState<SearchedUser[] | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const searchUsers = useCallback(async (keyword: string, teamKey: number) => {
    const trimmedKeyword = keyword.trim();

    // 빈 검색어면 결과 클리어
    if (trimmedKeyword.length === 0) {
      setSearchResults([]);
      setSearchError(null);
      return;
    }

    // 최소 2글자 이상
    if (trimmedKeyword.length < 2) {
      setSearchError('최소 2글자 이상 입력해주세요.');
      return;
    }

    setIsSearching(true);
    setSearchError(null);

    try {
      console.log('🔍 [useUserSearch] Searching users:', trimmedKeyword);


      const response = await apiController({
        url: `/api/v1/teams/${teamKey}/invite/search`,
        method: 'get',
        params: {
          keyword,
        }
      })
      console.log("검색 성공", response);
      setSearchResults(response.data.result);

    } catch (err) {
      const error = err as Error;
      console.error('❌ [useUserSearch] Search failed:', error);
      setSearchError('사용자 검색에 실패했습니다.');
      setSearchResults([]);
      Swal.fire({
        text: "검색에 실패했습니다.",
        icon: 'error',
        confirmButtonColor: '#1e50af',
      })
    } finally {
      setIsSearching(false);
    }
  }, []);

  const clearSearch = useCallback(() => {
    setSearchResults([]);
    setSearchError(null);
  }, []);

  return {
    searchResults,
    isSearching,
    searchError,
    searchUsers,
    clearSearch,
  };
};