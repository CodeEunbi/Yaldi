import type { WorkspaceTableColumn } from '../pages/workspace/WorkSpace';


// DTO 모델 타입 정의 (통합)



export interface DtoColumnInfo extends WorkspaceTableColumn {
  sourceTableId: string;
  sourceTableName: string;
  sourceTableIdentifier: string;
}



export type DataModelType = 'requestDto' | 'responseDto' | 'entity';
export type DataModelResponseType = "ENTITY" | "DTO_RESPONSE" | "DTO_REQUEST";

export type SyncStatus = 'IN_SYNC' | 'INVALID' | 'OUT_OF_SYNC';

export interface DataModelCode {
  java: string;
  typescript: string;
}

export interface DataModelColumn {
  columnKey: number;
  tableKey: number;
  tableName: string;
  logicalName: string;
  physicalName: string;
  dataType: string;
  dataDetail: string[] | null;
  isNullable: boolean;
  isPrimaryKey: boolean;
  isForeignKey: boolean;
  isUnique: boolean;
  alias: string;
}

export interface RelatedTable {
  tableKey: number;
  physicalName: string;
  logicalName: string;
  displayName: string;
}

// 통합 DTO 모델 타입. 받을 때
export interface DataModel {
  modelKey: number;
  projectKey: number;
  name: string;
  type: DataModelResponseType;
  syncStatus: SyncStatus;
  syncMessage: string;
  lastSyncedAt: string;
  relatedTables: RelatedTable[];
  createdAt: string;
  updatedAt: string;
  
  // 상세 조회에만 있는 필드들 (선택적)
  code?: DataModelCode;
  columns?: DataModelColumn[];
  
  // 목록 조회에만 있는 필드 (선택적)
  columnCount?: number;
}

// 만들 때
export interface DataModelItem {
  dataModelKey: number;
  name: string;
  type: DataModelType;
  description: string;
  relatedEntities: string[];
  createAt: Date;
  updatedAt?: Date;
  typeScript: string;
  java: string;
  sourceTableName?: string;
  fields?: DtoColumnInfo[];
}

