// 팀원을 이 프로젝트에서 내보내는 요청이 들어감.

// projectKey와 대상 멤버의 memberKey를 담아 delete 요청
// `/api/v1/projects/{projectKey}/members/{memberKey}` 여기로 delete 요청.
// 사용자의 role이 ADMIN 또는 OWNER 인 경우만 가능.
import { useState } from 'react';
import { apiController } from '../apis/apiController';

interface UseDeleteProjectMemberReturn {
  memberKeyToDelete: number | null;
  setMemberKeyToDelete: (key: number | null) => void;
  deleteProjectMember: (projectKey: number, memberKey : number) => void;
}

export const useDeleteProjectMember = (): UseDeleteProjectMemberReturn => {
  const [memberKeyToDelete, setMemberKeyToDelete] = useState<number | null>(null);

  const deleteProjectMember = async (projectKey: number, memberKey : number) => {

    try {
      const response = await apiController({
        url: `/api/v1/projects/${encodeURIComponent(projectKey)}/members/${encodeURIComponent(memberKey)}`,
        method: 'delete',
      })
      
      console.log(`${memberKey}번 멤버 방출 완료.`, response.data);
    } catch (err) {
      console.log("에러 발생:", err);
    }
  }


  return {
    memberKeyToDelete,
    setMemberKeyToDelete,
    deleteProjectMember
  };
};
