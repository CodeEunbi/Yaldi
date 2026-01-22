// project에 member 초대하는 훅.
// `/api/v1/projects/{projectKey}/members`으로 post 요청.
// 데이터는 members 넘김.
//   "members": [
//     {
//       "memberKey": 2,
//       "role": "EDITOR"
//     }
//   ]

import { useState } from 'react';
import { apiController } from "../apis/apiController";
import type { addProjectMemberRequest } from "../types/project";
import type { ApiError } from '../types/api';

interface UseAddProjectMemberReturn {
  addMembers: (projectKey: number, members: addProjectMemberRequest) => Promise<void>;
  isLoading: boolean;
  error?: string | null;
}

export const useAddProjectMember = (): UseAddProjectMemberReturn => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const addMembers = async (
    projectKey: number,
    members: addProjectMemberRequest,
  ): Promise<void> => {
    setIsLoading(true);
    setError(null);

    try {
      console.log('📨 [useAddProjectMember] Adding members to project:', projectKey, members);

      await apiController({
        url: `/api/v1/projects/${projectKey}/members`,
        method: 'post',
        data: members
      });

      console.log('✅ [useAddProjectMember] Members added successfully');

    } catch (err) {
      const error = err as ApiError;
      console.error('❌ [useAddProjectMember] Failed to add members:', error);

      let errorMessage = '멤버 추가에 실패했습니다.';

      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;

        switch (status) {
          case 409:
            errorMessage = '이미 프로젝트에 속한 멤버입니다.';
            break;
          case 404:
            errorMessage = '프로젝트를 찾을 수 없습니다.';
            break;
          case 403:
            errorMessage = '멤버를 추가할 권한이 없습니다.';
            break;
          default:
            errorMessage = data.message || errorMessage;
        }
      } else if (error.message) {
        errorMessage = error.message;
      }

      setError(errorMessage);
      throw new Error(errorMessage);

    } finally {
      setIsLoading(false);
    }
  };

  return {
    addMembers,
    isLoading,
    error,
  };
};
