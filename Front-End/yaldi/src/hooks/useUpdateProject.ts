// 기존 프로젝트 값과 다른 값을 받아 프로젝트 정보 update 요청 보내기.
// /hooks/useCreateProject와 유사하게.
// 팀장 권한이 있는 사람만 할 수 있음.

// `/api/v1/projects/{projectKey}` 로 patch 요청.
// name, description, imageUrl에 변화가 있는 경우에.

import { useState } from 'react';
import { apiController } from "../apis/apiController";
// import type { Project } from "../types/search";
import type { ApiError } from '../types/api';
import type { addProjectMemberRequestItem } from '../types/project';

interface UpdateProjectParams {
  projectKey: number;
  name?: string;
  description?: string;
  imageUrl?: string;
  originalMembers?: addProjectMemberRequestItem[];
  newMembers?: addProjectMemberRequestItem[];
}

interface UseUpdateProjectReturn {
  updateProject: (params: UpdateProjectParams) => Promise<void>;
  isLoading: boolean;
  error?: string | null;
}

export const useUpdateProject = (): UseUpdateProjectReturn => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const updateProject = async ({
    projectKey,
    name,
    description,
    imageUrl,
    originalMembers = [],
    newMembers = []
  }: UpdateProjectParams): Promise<void> => {
    setIsLoading(true);
    setError(null);

    try {
      // 1. 프로젝트 정보가 변경된 경우 PATCH 요청
      const hasProjectInfoChanged = name !== undefined || description !== undefined || imageUrl !== undefined;

      if (hasProjectInfoChanged) {
        console.log('📨 [useUpdateProject] Updating project:', { projectKey, name, description, imageUrl });

        const updateData: Record<string, string | undefined> = {};
        if (name !== undefined) updateData.name = name;
        if (description !== undefined) updateData.description = description;
        if (imageUrl !== undefined) updateData.imageUrl = imageUrl;

        await apiController({
          url: `/api/v1/projects/${projectKey}`,
          method: 'patch',
          data: updateData
        });

        console.log('✅ [useUpdateProject] Project updated successfully');
      }

      // 2. 멤버 변경사항 처리
      const originalMemberKeys = new Set(originalMembers.map(m => m.memberKey));
      const newMemberKeys = new Set(newMembers.map(m => m.memberKey));

      // 추가할 멤버 (새로운 멤버 목록에는 있지만 원래 멤버 목록에는 없는 경우)
      const membersToAdd = newMembers.filter(m => !originalMemberKeys.has(m.memberKey));

      // 삭제할 멤버 (원래 멤버 목록에는 있지만 새로운 멤버 목록에는 없는 경우)
      const membersToDelete = originalMembers.filter(m => !newMemberKeys.has(m.memberKey));

      // 멤버 추가 요청
      if (membersToAdd.length > 0) {
        console.log('📨 [useUpdateProject] Adding members:', membersToAdd);

        await apiController({
          url: `/api/v1/projects/${projectKey}/members`,
          method: 'post',
          data: {
            members: membersToAdd
          }
        });

        console.log('✅ [useUpdateProject] Members added successfully');
      }

      // 멤버 삭제 요청 (각 멤버별로 개별 DELETE 요청)
      if (membersToDelete.length > 0) {
        console.log('📨 [useUpdateProject] Removing members:', membersToDelete);

        await Promise.all(
          membersToDelete.map(member =>
            apiController({
              url: `/api/v1/projects/${projectKey}/members/${member.memberKey}`,
              method: 'delete'
            })
          )
        );

        console.log('✅ [useUpdateProject] Members removed successfully');
      }

    } catch (err) {
      const error = err as ApiError;
      console.error('❌ [useUpdateProject] Failed to update project:', error);

      let errorMessage = '프로젝트 수정에 실패했습니다.';

      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;

        switch (status) {
          case 400:
            errorMessage = '잘못된 요청입니다. 입력값을 확인해주세요.';
            break;
          case 404:
            errorMessage = '프로젝트를 찾을 수 없습니다.';
            break;
          case 403:
            errorMessage = '프로젝트를 수정할 권한이 없습니다.';
            break;
          case 409:
            errorMessage = '이미 프로젝트에 속한 멤버입니다.';
            break;
          default:
            errorMessage = data.message || errorMessage;
        }
      } else if (error.message) {
        errorMessage = error.message;
      }

      setError(errorMessage);
      throw error;

    } finally {
      setIsLoading(false);
    }
  };

  return {
    updateProject,
    isLoading,
    error
  };
};