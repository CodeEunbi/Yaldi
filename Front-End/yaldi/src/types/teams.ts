export interface Team {
  teamKey: number;
  ownedBy: number;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface TeamMember {
  userKey: number;
  isOwner: boolean;
  nickname?: string;
  nickName?: string;
  email?: string;
  updatedAt?: string;
  invitedAt?: string;
}

export interface Invitations {
  invitationKey: number;
  teamKey: number;
  teamName: string;
  inviterUserKey: number;
  inviterNickname: string;
  invitedUserKey: number;
  invitedNickname: string;
  invitedEmail: string;
  status: string;
  createdAt: string;
}

export interface TeamProject {
  projectKey: number;
  teamKey: number;
  name: string;
  description?: string;
  imageUrl?: string | null;
  ownedBy?: string;
  createdAt: string;
  updatedAt: string;
  isMember: boolean;
  role?: "OWNER" | "ADMIN" | "EDITOR" | null;
}

export interface ModalState {
  editTeam: boolean;
  deleteTeam: boolean;
  inviteMember: boolean;
  removeMember: { isOpen: boolean; userKey: number | null };
  transferOwnership: { isOpen: boolean; userKey: number | null };
  createProject: boolean;
  deleteProject: { isOpen: boolean; projectKey: number | null };
  memberOption: { isOpen: boolean; userKey: number | null };
  invitationOption: { isOpen: boolean; invitationKey: number | null };
}

export interface CreateProjectData {
  name: string;
  description?: string;
  useAI: boolean;
  thumbnail?: File;
}


export interface getTeamMemberListResponse {
  "userKey": number;
  "nickName": string;
  "isOwner": boolean;

}
// export interface PaginationInfo {
//   currentPage: number;
//   size: number;
//   totalElements: number;
//   hasNext: boolean;
// }
