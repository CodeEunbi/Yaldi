// ============================================
// 기본 스키마 타입 정의
// ============================================

interface Column {
  dataType: string;
  isUnique: boolean;
  columnKey: number;
  dataDetail: (string | number)[];
  isNullable: boolean;
  logicalName: string;
  defaultValue: string | null;
  isForeignKey: boolean;
  isPrimaryKey: boolean;
  physicalName: string;
  isIncremental: boolean;
}

interface Table {
  columns: Column[];
  tableKey: number;
  logicalName: string;
  physicalName: string;
}

interface Relation {
  toTableKey: number;
  fromTableKey: number;
  relationType: string;
  constraintName: string;
  onDeleteAction: string;
  onUpdateAction: string;
}

interface SchemaData {
  tables: Table[];
  relations: Relation[];
}

interface Version {
  versionKey: number;
  projectKey: number;
  name: string;
  description: string;
  schemaData: SchemaData;
  isPublic: boolean;
  designVerificationStatus: string;
  verificationErrors: string[];
  verificationWarnings: string[];
  verificationMessage: string;
  verificationSuggestions: string[];
  createdAt: string;
  updatedAt: string;
}

// ============================================
// Diff 타입 정의
// ============================================

type ChangeType = 'ADDED' | 'MODIFIED' | 'DELETED' | 'UNCHANGED';

// 컬럼의 변경 가능한 필드들만 추출
type ColumnChangeableFields = Omit<Column, 'columnKey'>;

// 테이블의 변경 가능한 필드들만 추출
type TableChangeableFields = Pick<Table, 'logicalName' | 'physicalName'>;

// 관계의 변경 가능한 필드들만 추출
type RelationChangeableFields = Pick<Relation, 'onDeleteAction' | 'onUpdateAction' | 'relationType' | 'constraintName'>;

interface ColumnDiff extends Column {
  changeType: ChangeType;
  changedFields: (keyof ColumnChangeableFields)[] | null;
  previousValues: Partial<ColumnChangeableFields> | null;
}

interface TableDiff {
  changeType: ChangeType;
  tableKey: number;
  physicalName: string;
  logicalName: string;
  columnDiffs: ColumnDiff[];
  changedFields: (keyof TableChangeableFields)[] | null;
  previousValues: Partial<TableChangeableFields> | null;
}

interface RelationDiff extends Relation {
  changeType: ChangeType;
  changedFields: (keyof RelationChangeableFields)[] | null;
  previousValues: Partial<RelationChangeableFields> | null;
}

interface SchemaDiffSummary {
  addedTables: number;
  modifiedTables: number;
  deletedTables: number;
  addedColumns: number;
  modifiedColumns: number;
  deletedColumns: number;
  addedRelations: number;
  modifiedRelations: number;
  deletedRelations: number;
  hasChanges: boolean;
}

interface SchemaDiff {
  tableDiffs: TableDiff[];
  relationDiffs: RelationDiff[];
  summary: SchemaDiffSummary;
}

interface Diff {
  previousVersion: Version;
  currentVersion: Version;
  schemaDiff: SchemaDiff;
}

// ============================================
// 파싱된 결과 타입 정의
// ============================================

type FieldValue = string | number | boolean | (string | number)[] | null;

interface FieldChange {
  field: string;
  before: FieldValue;
  after: FieldValue;
  color: string;
  displayBefore: string;
  displayAfter: string;
}

interface ParsedColumnChange extends Column {
  changeType: ChangeType;
  fieldChanges: FieldChange[];
  color: string;
}

interface ParsedTableChange {
  changeType: ChangeType;
  tableKey: number;
  physicalName: string;
  logicalName: string;
  columns: ParsedColumnChange[];
  tableFieldChanges: FieldChange[];
  color: string;
}

interface ParsedRelationChange extends Relation {
  changeType: ChangeType;
  fieldChanges: FieldChange[];
  color: string;
}

interface ParsedDiff {
  tables: ParsedTableChange[];
  relations: ParsedRelationChange[];
  summary: SchemaDiffSummary;
}

// ============================================
// 색상 정의
// ============================================

interface ColorScheme {
  bg: string;
  text: string;
  border: string;
  hex: string;
  light: string;
}

const COLORS: Record<ChangeType, ColorScheme> = {
  ADDED: {
    bg: 'bg-[#009e73]',
    text: 'text-green-700',
    border: 'border-green-300',
    hex: '#28A745', // 이거 씀
    light: '#F0FDF4',
  },
  MODIFIED: {
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    border: 'border-amber-300',
    hex: '#FFD166',
    light: '#FFFBEB',
  },
  DELETED: {
    bg: 'bg-red-50',
    text: 'text-red-700',
    border: 'border-red-300',
    hex: '#DC3545',
    light: '#FEF2F2',
  },
  UNCHANGED: {
    bg: 'bg-gray-50',
    text: 'text-gray-700',
    border: 'border-gray-300',
    hex: '#6B7280',
    light: '#F9FAFB',
  },
};

// ============================================
// 유틸리티 함수
// ============================================

/**
 * 필드명을 한글로 변환
 */
function getFieldDisplayName(field: string): string {
  const fieldNames: Record<string, string> = {
    logicalName: '논리명',
    physicalName: '물리명',
    dataType: '데이터 타입',
    dataDetail: '데이터 상세',
    isPrimaryKey: 'PK',
    isNullable: 'NULL 허용',
    isUnique: 'UNIQUE',
    isForeignKey: 'FK',
    isIncremental: 'AUTO_INCREMENT',
    defaultValue: '기본값',
    onDeleteAction: 'ON DELETE',
    onUpdateAction: 'ON UPDATE',
    relationType: '관계 타입',
    constraintName: '제약조건명',
  };
  
  return fieldNames[field] || field;
}

/**
 * 필드 값을 사람이 읽기 쉬운 형태로 변환
 */
function formatFieldValue(field: string, value: FieldValue): string {
  if (value === null || value === undefined) {
    return 'NULL';
  }
  
  if (Array.isArray(value)) {
    if (field === 'dataDetail') {
      return value.join(', ');
    }
    return JSON.stringify(value);
  }
  
  if (typeof value === 'boolean') {
    return value ? 'YES' : 'NO';
  }
  
  return String(value);
}


/**
 * 컬럼 diff에서 필드 변경사항 생성
 */
function createColumnFieldChanges(
  columnDiff: ColumnDiff
): FieldChange[] {
  const fieldChanges: FieldChange[] = [];
  
  if (!columnDiff.changedFields || !columnDiff.previousValues) {
    return fieldChanges;
  }
  
  columnDiff.changedFields.forEach(field => {
    const beforeValue = columnDiff.previousValues![field];
    const afterValue = columnDiff[field];
    
    if (beforeValue !== undefined) {
      fieldChanges.push({
        field: getFieldDisplayName(field),
        before: beforeValue as FieldValue,
        after: afterValue as FieldValue,
        color: COLORS.MODIFIED.hex,
        displayBefore: formatFieldValue(field, beforeValue as FieldValue),
        displayAfter: formatFieldValue(field, afterValue as FieldValue),
      });
    }
  });
  
  return fieldChanges;
}

/**
 * 테이블 diff에서 필드 변경사항 생성
 */
function createTableFieldChanges(
  tableDiff: TableDiff
): FieldChange[] {
  const fieldChanges: FieldChange[] = [];
  
  if (!tableDiff.changedFields || !tableDiff.previousValues) {
    return fieldChanges;
  }
  
  tableDiff.changedFields.forEach(field => {
    const beforeValue = tableDiff.previousValues![field];
    const afterValue = tableDiff[field];
    
    if (beforeValue !== undefined) {
      fieldChanges.push({
        field: getFieldDisplayName(field),
        before: beforeValue,
        after: afterValue,
        color: COLORS.MODIFIED.hex,
        displayBefore: formatFieldValue(field, beforeValue),
        displayAfter: formatFieldValue(field, afterValue),
      });
    }
  });
  
  return fieldChanges;
}

/**
 * 관계 diff에서 필드 변경사항 생성
 */
function createRelationFieldChanges(
  relationDiff: RelationDiff
): FieldChange[] {
  const fieldChanges: FieldChange[] = [];
  
  if (!relationDiff.changedFields || !relationDiff.previousValues) {
    return fieldChanges;
  }
  
  relationDiff.changedFields.forEach(field => {
    const beforeValue = relationDiff.previousValues![field];
    const afterValue = relationDiff[field];
    
    if (beforeValue !== undefined) {
      fieldChanges.push({
        field: getFieldDisplayName(field),
        before: beforeValue,
        after: afterValue,
        color: COLORS.MODIFIED.hex,
        displayBefore: formatFieldValue(field, beforeValue),
        displayAfter: formatFieldValue(field, afterValue),
      });
    }
  });
  
  return fieldChanges;
}

/**
 * 컬럼 diff를 파싱된 컬럼으로 변환
 */
function parseColumnDiff(columnDiff: ColumnDiff): ParsedColumnChange {
  const columnColor = COLORS[columnDiff.changeType];
  const fieldChanges = createColumnFieldChanges(columnDiff);
  
  return {
    changeType: columnDiff.changeType,
    columnKey: columnDiff.columnKey,
    physicalName: columnDiff.physicalName,
    logicalName: columnDiff.logicalName,
    dataType: columnDiff.dataType,
    dataDetail: columnDiff.dataDetail,
    isPrimaryKey: columnDiff.isPrimaryKey,
    isNullable: columnDiff.isNullable,
    isUnique: columnDiff.isUnique,
    isForeignKey: columnDiff.isForeignKey,
    isIncremental: columnDiff.isIncremental,
    defaultValue: columnDiff.defaultValue,
    fieldChanges,
    color: columnColor.hex,
  };
}

/**
 * 테이블 diff를 파싱된 테이블로 변환
 */
function parseTableDiff(tableDiff: TableDiff): ParsedTableChange {
  const tableColor = COLORS[tableDiff.changeType];
  const tableFieldChanges = createTableFieldChanges(tableDiff);
  
  // 컬럼 변경사항 파싱 (UNCHANGED 제외)
  const parsedColumns: ParsedColumnChange[] = tableDiff.columnDiffs
    .filter(columnDiff => {
      // UNCHANGED 컬럼은 테이블이 ADDED가 아닌 경우 제외
      return !(columnDiff.changeType === 'UNCHANGED' && tableDiff.changeType !== 'ADDED');
    })
    .map(columnDiff => parseColumnDiff(columnDiff));
  
  return {
    changeType: tableDiff.changeType,
    tableKey: tableDiff.tableKey,
    physicalName: tableDiff.physicalName,
    logicalName: tableDiff.logicalName,
    columns: parsedColumns,
    tableFieldChanges,
    color: tableColor.hex,
  };
}

/**
 * 관계 diff를 파싱된 관계로 변환
 */
function parseRelationDiff(relationDiff: RelationDiff): ParsedRelationChange {
  const relationColor = COLORS[relationDiff.changeType];
  const fieldChanges = createRelationFieldChanges(relationDiff);
  
  return {
    changeType: relationDiff.changeType,
    fromTableKey: relationDiff.fromTableKey,
    toTableKey: relationDiff.toTableKey,
    relationType: relationDiff.relationType,
    constraintName: relationDiff.constraintName,
    onDeleteAction: relationDiff.onDeleteAction,
    onUpdateAction: relationDiff.onUpdateAction,
    fieldChanges,
    color: relationColor.hex,
  };
}

// ============================================
// 메인 파싱 함수
// ============================================

/**
 * schemaDiff를 파싱해서 변경사항만 추출하고 색상 정보 추가
 */
export function parseSchemaDiff(diff: Diff): ParsedDiff {
  // UNCHANGED가 아닌 테이블만 필터링하고 파싱
  const parsedTables: ParsedTableChange[] = diff.schemaDiff.tableDiffs
    .filter(tableDiff => tableDiff.changeType !== 'UNCHANGED')
    .map(tableDiff => parseTableDiff(tableDiff));
  
  // 모든 관계 변경사항 파싱
  const parsedRelations: ParsedRelationChange[] = diff.schemaDiff.relationDiffs
    .map(relationDiff => parseRelationDiff(relationDiff));
  
  return {
    tables: parsedTables,
    relations: parsedRelations,
    summary: diff.schemaDiff.summary,
  };
}

/**
 * 테이블명 찾기 (relationDiff에서 사용)
 */
export function getTableName(
  tableKey: number,
  currentVersion: Version
): string {
  const table = currentVersion.schemaData.tables.find(
    (t: Table) => t.tableKey === tableKey
  );
  return table ? `${table.logicalName} (${table.physicalName})` : `Table ${tableKey}`;
}

/**
 * 변경 타입별 통계 계산
 */
export function getChangeStats(parsedDiff: ParsedDiff): {
  tables: Record<ChangeType, number>;
  columns: Record<ChangeType, number>;
  relations: Record<ChangeType, number>;
} {
  const tableStats: Record<ChangeType, number> = {
    ADDED: 0,
    MODIFIED: 0,
    DELETED: 0,
    UNCHANGED: 0,
  };
  
  const columnStats: Record<ChangeType, number> = {
    ADDED: 0,
    MODIFIED: 0,
    DELETED: 0,
    UNCHANGED: 0,
  };
  
  const relationStats: Record<ChangeType, number> = {
    ADDED: 0,
    MODIFIED: 0,
    DELETED: 0,
    UNCHANGED: 0,
  };
  
  // 테이블 통계
  parsedDiff.tables.forEach(table => {
    tableStats[table.changeType]++;
    
    // 컬럼 통계
    table.columns.forEach(column => {
      columnStats[column.changeType]++;
    });
  });
  
  // 관계 통계
  parsedDiff.relations.forEach(relation => {
    relationStats[relation.changeType]++;
  });
  
  return {
    tables: tableStats,
    columns: columnStats,
    relations: relationStats,
  };
}

// ============================================
// Export 타입들
// ============================================

export type {
  Diff,
  ParsedDiff,
  ParsedTableChange,
  ParsedColumnChange,
  ParsedRelationChange,
  FieldChange,
  ChangeType,
  ColorScheme,
  SchemaDiffSummary,
};

export { COLORS };