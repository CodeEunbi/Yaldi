export interface PaginationInfo {
  currentPage: number;
  size: number;
  totalElements: number;
  hasNext: boolean;
};

export interface LongPaginationInfo {
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
