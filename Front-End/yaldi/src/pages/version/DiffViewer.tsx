// components/DiffViewer.tsx
import React from 'react';
import {
  type Diff,
  type ParsedDiff,
  type ParsedTableChange,
  type ParsedColumnChange,
  type ParsedRelationChange,
  type FieldChange,
  COLORS,
  parseSchemaDiff,
  getTableName,
  getChangeStats,
} from '../../utils/schemaDiffParser';

interface DiffViewerProps {
  diff: Diff;
}

/**
 * 변경 타입 뱃지 컴포넌트
 */
const ChangeTypeBadge: React.FC<{ changeType: 'ADDED' | 'MODIFIED' | 'DELETED' | 'UNCHANGED' }> = ({ changeType }) => {
  const color = COLORS[changeType];
  
  const labels = {
    ADDED: '추가',
    MODIFIED: '수정',
    DELETED: '삭제',
    UNCHANGED: '유지',
  };
  
  return (
    <span
      className="px-2 py-1 rounded text-xs font-bold text-white"
      style={{ backgroundColor: color.hex }}
    >
      {labels[changeType]}
    </span>
  );
};

/**
 * 필드 변경사항 표시 컴포넌트
 */
const FieldChangeDisplay: React.FC<{ changes: FieldChange[] }> = ({ changes }) => {
  if (changes.length === 0) {
    return <span className="text-gray-400 text-xs">-</span>;
  }
  
  return (
    <div className="space-y-1">
      {changes.map((change, idx) => (
        <div key={idx} className="text-xs flex items-center gap-1 flex-wrap">
          <span className="font-medium text-gray-700">{change.field}:</span>
          <span className="line-through text-red-600">
            {change.displayBefore}
          </span>
          <span className="text-gray-400">→</span>
          <span className="text-green-600 font-medium">
            {change.displayAfter}
          </span>
        </div>
      ))}
    </div>
  );
};

/**
 * 제약조건 뱃지들
 */
const ConstraintBadges: React.FC<{ column: ParsedColumnChange }> = ({ column }) => {
  return (
    <div className="flex gap-1 flex-wrap">
      {column.isPrimaryKey && (
        <span className="bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded text-xs font-semibold">
          PK
        </span>
      )}
      {column.isForeignKey && (
        <span className="bg-purple-100 text-purple-700 px-1.5 py-0.5 rounded text-xs font-semibold">
          FK
        </span>
      )}
      {column.isUnique && (
        <span className="bg-green-100 text-green-700 px-1.5 py-0.5 rounded text-xs font-semibold">
          UQ
        </span>
      )}
      {!column.isNullable && (
        <span className="bg-red-100 text-red-700 px-1.5 py-0.5 rounded text-xs font-semibold">
          NN
        </span>
      )}
      {column.isIncremental && (
        <span className="bg-indigo-100 text-indigo-700 px-1.5 py-0.5 rounded text-xs font-semibold">
          AI
        </span>
      )}
    </div>
  );
};

/**
 * 변경 요약 섹션
 */
const ChangeSummary: React.FC<{ parsedDiff: ParsedDiff }> = ({ parsedDiff }) => {
  const stats = getChangeStats(parsedDiff);
  
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm">
      <h3 className="font-bold text-lg mb-4 text-gray-900">변경 요약</h3>
      
      <div className="space-y-3">
        {/* 테이블 통계 */}
        <div className=''>
          <div className="grid grid-cols-4 gap-4 text-sm mb-7">
          <div className="text-sm font-semibold text-gray-700">테이블</div>
            <div className="text-center">
              <div className="text-green-600 font-bold text-xl">
                +{stats.tables.ADDED}
              </div>
              <div className="text-gray-600 text-xs">추가</div>
            </div>
            <div className="text-center">
              <div className="text-amber-600 font-bold text-xl">
                ~{stats.tables.MODIFIED}
              </div>
              <div className="text-gray-600 text-xs">수정</div>
            </div>
            <div className="text-center">
              <div className="text-red-600 font-bold text-xl">
                -{stats.tables.DELETED}
              </div>
              <div className="text-gray-600 text-xs">삭제</div>
            </div>
          </div>
        </div>
        
        {/* 컬럼 통계 */}
        <div>
          <div className="grid grid-cols-4 gap-4 text-sm mb-7">
          <div className="text-sm font-semibold text-gray-700">컬럼</div>
            <div className="text-center">
              <div className="text-green-600 font-bold text-xl">
                +{stats.columns.ADDED}
              </div>
              <div className="text-gray-600 text-xs">추가</div>
            </div>
            <div className="text-center">
              <div className="text-amber-600 font-bold text-xl">
                ~{stats.columns.MODIFIED}
              </div>
              <div className="text-gray-600 text-xs">수정</div>
            </div>
            <div className="text-center">
              <div className="text-red-600 font-bold text-xl">
                -{stats.columns.DELETED}
              </div>
              <div className="text-gray-600 text-xs">삭제</div>
            </div>
          </div>
        </div>
        
        {/* 관계 통계 */}
        <div>
          <div className="grid grid-cols-4 gap-4 text-sm">
          <div className="text-sm font-semibold text-gray-700">관계</div>
            <div className="text-center">
              <div className="text-green-600 font-bold text-xl">
                +{stats.relations.ADDED}
              </div>
              <div className="text-gray-600 text-xs">추가</div>
            </div>
            <div className="text-center">
              <div className="text-amber-600 font-bold text-xl">
                ~{stats.relations.MODIFIED}
              </div>
              <div className="text-gray-600 text-xs">수정</div>
            </div>
            <div className="text-center">
              <div className="text-red-600 font-bold text-xl">
                -{stats.relations.DELETED}
              </div>
              <div className="text-gray-600 text-xs">삭제</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

/**
 * 테이블 변경사항 섹션
 */
const TableChanges: React.FC<{ table: ParsedTableChange }> = ({ table }) => {
  return (
    <div
      className="border-2 rounded-lg overflow-hidden shadow-sm"
      style={{ borderColor: table.color }}
    >
      {/* 테이블 헤더 */}
      <div
        className="p-4 flex items-center justify-between"
        style={{ backgroundColor: `${table.color}15` }}
      >
        <div className="flex items-center gap-3">
          <ChangeTypeBadge changeType={table.changeType} />
          <span className="font-bold text-gray-900">
            {table.logicalName}
          </span>
          <span className="text-gray-500 text-sm font-mono">
            ({table.physicalName})
          </span>
        </div>
      </div>
      
      {/* 테이블 레벨 변경사항 */}
      {table.tableFieldChanges.length > 0 && (
        <div className="px-4 py-3 bg-amber-50 border-y border-amber-200">
          <div className="text-sm font-semibold mb-2 text-amber-900">
            테이블 속성 변경:
          </div>
          <FieldChangeDisplay changes={table.tableFieldChanges} />
        </div>
      )}
      
      {/* 컬럼 목록 */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="p-3 text-left text-xs font-semibold text-gray-700 min-w-[80px]">
                상태
              </th>
              <th className="p-3 text-left text-xs font-semibold text-gray-700 min-w-[120px]">
                논리명
              </th>
              <th className="p-3 text-left text-xs font-semibold text-gray-700 min-w-[120px]">
                물리명
              </th>
              <th className="p-3 text-left text-xs font-semibold text-gray-700 min-w-[150px]">
                타입
              </th>
              <th className="p-3 text-left text-xs font-semibold text-gray-700 min-w-[100px]">
                제약조건
              </th>
              <th className="p-3 text-left text-xs font-semibold text-gray-700 min-w-[200px]">
                변경내용
              </th>
            </tr>
          </thead>
          <tbody>
            {table.columns.map(column => (
              <tr
                key={column.columnKey}
                className="border-b border-gray-100 hover:bg-gray-50 transition-colors"
                style={{
                  backgroundColor:
                    column.changeType !== 'UNCHANGED'
                      ? `${column.color}08`
                      : 'transparent',
                }}
              >
                <td className="p-3">
                  <ChangeTypeBadge changeType={column.changeType} />
                </td>
                <td className="p-3">
                  <span className="text-sm font-medium text-gray-900">
                    {column.logicalName}
                  </span>
                </td>
                <td className="p-3">
                  <span className="text-gray-600 font-mono text-sm">
                    {column.physicalName}
                  </span>
                </td>
                <td className="p-3">
                  <span className="font-mono text-sm text-gray-900">
                    {column.dataType}
                    {column.dataDetail.length > 0 && (
                      <span className="text-gray-500">
                        ({column.dataDetail.join(', ')})
                      </span>
                    )}
                  </span>
                  {column.defaultValue && (
                    <div className="text-xs text-gray-500 mt-1">
                      기본값: <span className="font-mono">{column.defaultValue}</span>
                    </div>
                  )}
                </td>
                <td className="p-3">
                  <ConstraintBadges column={column} />
                </td>
                <td className="p-3">
                  <FieldChangeDisplay changes={column.fieldChanges} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

/**
 * 관계 변경사항 섹션
 */
const RelationChanges: React.FC<{
  relations: ParsedRelationChange[];
  currentVersion: Diff['currentVersion'];
}> = ({ relations, currentVersion }) => {
  if (relations.length === 0) {
    return null;
  }
  
  return (
    <div className="space-y-3">
      <h3 className="font-bold text-lg text-gray-900">관계 변경사항</h3>
      {relations.map((relation, idx) => (
        <div
          key={idx}
          className="border-2 rounded-lg p-4 shadow-sm"
          style={{
            borderColor: relation.color,
            backgroundColor: `${relation.color}08`,
          }}
        >
          <div className="flex items-center gap-3 mb-3">
            <ChangeTypeBadge changeType={relation.changeType} />
            <span className="font-mono text-sm font-semibold text-gray-900">
              {relation.constraintName}
            </span>
          </div>
          
          <div className="flex items-center gap-2 mb-3 text-sm">
            <span className="bg-white px-3 py-1 rounded border border-gray-300 font-medium">
              {getTableName(relation.fromTableKey, currentVersion)}
            </span>
            <span className="text-gray-400">→</span>
            <span className="bg-white px-3 py-1 rounded border border-gray-300 font-medium">
              {getTableName(relation.toTableKey, currentVersion)}
            </span>
          </div>
          
          <div className="grid grid-cols-2 gap-2 text-sm mb-3">
            <div>
              <span className="text-gray-600">관계 타입:</span>{' '}
              <span className="font-mono font-medium">{relation.relationType}</span>
            </div>
            <div>
              <span className="text-gray-600">ON DELETE:</span>{' '}
              <span className="font-mono font-medium">{relation.onDeleteAction}</span>
            </div>
            <div className="col-span-2">
              <span className="text-gray-600">ON UPDATE:</span>{' '}
              <span className="font-mono font-medium">{relation.onUpdateAction}</span>
            </div>
          </div>
          
          {relation.fieldChanges.length > 0 && (
            <div className="border-t border-gray-200 pt-3 mt-3">
              <div className="text-sm font-semibold mb-2 text-gray-700">
                변경사항:
              </div>
              <FieldChangeDisplay changes={relation.fieldChanges} />
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

/**
 * 메인 DiffViewer 컴포넌트
 */
const DiffViewer: React.FC<DiffViewerProps> = ({ diff }) => {
  const parsedDiff = React.useMemo(() => parseSchemaDiff(diff), [diff]);
  
  if (!parsedDiff.summary.hasChanges) {
    return (
      <div className="bg-gray-50 rounded-lg border border-gray-200 p-8 text-center">
        <div className="text-gray-400 text-lg">변경사항이 없습니다.</div>
      </div>
    );
  }
  
  return (
    <div className="space-y-6">
      {/* 변경 요약 */}
      <ChangeSummary parsedDiff={parsedDiff} />
      
      {/* 테이블 변경사항 */}
      {parsedDiff.tables.length > 0 && (
        <div className="space-y-4">
          <h3 className="font-bold text-lg text-gray-900">
            테이블 변경사항 ({parsedDiff.tables.length})
          </h3>
          {parsedDiff.tables.map(table => (
            <TableChanges key={table.tableKey} table={table} />
          ))}
        </div>
      )}
      
      {/* 관계 변경사항 */}
      <RelationChanges
        relations={parsedDiff.relations}
        currentVersion={diff.currentVersion}
      />
    </div>
  );
};

export default DiffViewer;