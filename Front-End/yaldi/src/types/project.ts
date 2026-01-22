export interface createProjectRequest {
  teamKey: number;
  name: string;
  description?: string;
  imageUrl?: string;
}

export interface addProjectMemberRequestItem {
  memberKey: number;
  role: "OWNER" | "EDITOR" | "ADMIN";
}

export interface addProjectMemberRequest {
  members: addProjectMemberRequestItem[];
}

export interface getProjectMemberListResponseItem {
  "projectMemberRelationKey": number
  "memberKey": number;
  "nickname": string;
  "email": string;
  "role": string;
  "createdAt": string;
}

export interface getProjectMemberListResponseMeta {
  "page": number;
  "size": number;
  "numberOfElements": number;
  "totalElements": number;
  "totalPages": number;
  "first": boolean;
  "last": boolean;
  "empty": boolean;
  "sort": {
    "sorted": boolean;
    "unsorted": boolean;
    "empty": boolean;
  }
}

export interface getProjectMemberListResponse {
  data: getProjectMemberListResponseItem[];
  meta: getProjectMemberListResponseMeta;
}

export interface projectInfo {
  "projectKey": number;
  "teamKey": number;
  "name": string;
  "description"?: string;
  "imageUrl"?: string;
  "createdAt"?: string;
  "updatedAt": string;
  "lastActivityAt"?: string;
  "isMember": boolean;
  "role"?: string;
};
