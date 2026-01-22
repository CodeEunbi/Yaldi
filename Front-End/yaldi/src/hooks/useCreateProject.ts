import { useState } from 'react';
import { apiController } from "../apis/apiController";
import type { Project } from "../types/search";
import type { ApiError } from '../types/api';
import Swal from 'sweetalert2';

interface UseCreateProjectReturn {
  createProject: (teamKey: number, name: string, description?: string, imageUrl?: string) => Promise<Project>;
  isCreating: boolean;
  createError?: string | null;
}

export const useCreateProject = (): UseCreateProjectReturn => {
  const [isCreating, setIsCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const createProject = async (
    teamKey: number,
    name: string,
    description?: string,
    imageUrl?: string
  ): Promise<Project> => {
    setIsCreating(true);
    setCreateError(null);

    try {
      console.log('📨 [useCreateProject] Creating project:', { teamKey, name, description, imageUrl });

      const response = await apiController({
        url: '/api/v1/projects',
        method: 'post',
        data: {
          teamKey,
          name,
          description,
          imageUrl
        }
      });

      console.log('프로젝트 생성 성공.');
      Swal.fire({
        text: '프로젝트가 생성되었습니다.',
        icon: 'success',
        timer: 2000,
        showConfirmButton: false,
        timerProgressBar: true,
      })
      return response.data.result;

    } catch (err: any) {
      const error = err as ApiError;
      console.error('프로젝트 생성 중 에러 발생:', error);
      Swal.fire({
        // text: err.data.result ,
        text: err.data.message,
        confirmButtonColor: '#1e50af',
        icon: 'error',
      })


      let errorMessage = '프로젝트 생성에 실패했습니다.';

      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;

        switch (status) {
          case 400:
            errorMessage = '잘못된 요청입니다. 입력값을 확인해주세요.';
            break;
          case 404:
            errorMessage = '팀을 찾을 수 없습니다.';
            break;
          case 403:
            errorMessage = '프로젝트를 생성할 권한이 없습니다.';
            break;
          default:
            errorMessage = data.message || errorMessage;
        }
      } else if (error.message) {
        errorMessage = error.message;
      }

      setCreateError(errorMessage);
      throw error;

    } finally {
      setIsCreating(false);
    }
  };

  return {
    createProject,
    isCreating,
    createError,
  };
};