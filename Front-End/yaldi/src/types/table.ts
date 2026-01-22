// 테이블 생성 요청 타입
export interface CreateTableRequest {
  projectKey: number;
  logicalName: string;
  physicalName: string;
  xPosition: number;
  yPosition: number;
  colorHex: string;
}

// 테이블 생성 응답 타입
export interface CreateTableResponse {
  tableKey: number;
  logicalName: string;
  physicalName: string;
  xPosition: number;
  yPosition: number;
  colorHex: string;
}
