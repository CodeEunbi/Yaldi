// hooks/useInviteMember.ts

import { useState } from 'react';
import { apiController } from '../apis/apiController';
import { type Invitations } from '../types/teams';
import type { ApiError } from '../types/api';

interface UseInviteMemberReturn {
  inviteMember: (teamKey: number, targetUserKey: number) => Promise<Invitations>;
  isInviting: boolean;
  inviteError?: string | null;
}

export const useInviteMember = (): UseInviteMemberReturn => {
  const [isInviting, setIsInviting] = useState(false);
  const [inviteError, setInviteError] = useState<string | null>(null);

  const inviteMember = async (teamKey: number, targetUserKey: number): Promise<Invitations> => {
    setIsInviting(true);
    setInviteError(null);

    try {
      console.log('팀에 초대함.', teamKey, "에", targetUserKey);

      const response = await apiController({
        url: `/api/v1/teams/${teamKey}/invite`,
        method: 'post',
        data: {
          targetUserKey
        }
      })
      console.log('✅ [useInviteMember] Invitation sent successfully');

      return response.data.result;


    } catch (err) {

      const error = err as ApiError;

      console.error('❌ [useInviteMember] Failed to invite:', error);

      let errorMessage = '멤버 초대에 실패했습니다.';

      const data = error.response!.data;

      errorMessage = data.message || errorMessage;

      setInviteError(errorMessage);

      throw new Error(errorMessage);

    } finally {
      setIsInviting(false);
    }
  };

  return {
    inviteMember,
    isInviting,
    inviteError,
  };
};