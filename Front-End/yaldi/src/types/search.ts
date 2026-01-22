export interface Project {
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

export interface PageInfo {
  totalElements: number;
  currentPage: number;
  size: number;
  hasNext: boolean;
}

export interface SearchResultResponse {
  data: Project[];
  meta: PageInfo;
}