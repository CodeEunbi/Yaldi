// 마이페이지 관련 타입 정의

// 사용자 정보 인터페이스
export interface UserInfo {
  userKey: number;
  email: string;
  nickname: string;
  createdAt: string;
  updatedAt: string;
}


export interface TeamItem {
  teamKey: number;
  ownedBy: number;
  name: string;
  createdAt: string;
  updatedAt: string;
}


export interface TeamList {
  teams: TeamItem[];
}


export interface ProjectItem {
  projectKey: number;
  teamKey: number;
  name: string;
  description: string;
  imageUrl: string;
  createdAt: string;
  updatedAt: string;
  lastActivityAt: string;
  isMember: true,
  role: string;
}


export interface ProjectList {
  projects: ProjectItem[];
}