import { useState } from 'react';
import type { WorkspaceRelation, WorkspaceTable } from '../WorkSpace';
import relationOne from '../../../assets/icons/relation-one.svg';
import relationOneOnly from '../../../assets/icons/relation-one-only.svg';
import relationOneOrMany from '../../../assets/icons/relation-one-or-many.svg';
import relationZeroOrOne from '../../../assets/icons/relation-zero-or-one.svg';
import relationZeroOrMany from '../../../assets/icons/relation-zero-or-many.svg';
import relationZeroOrOneOrMany from '../../../assets/icons/relation-zero-or-one-or-many.svg';

type RelationLineProps = {
  relation: WorkspaceRelation;
  tables: WorkspaceTable[];
  onDelete?: (relationId: string) => void;
  onHoverChange?: (relationId: string | null) => void;
};

// 테이블 크기 상수
const TABLE_WIDTH = 300;
const TABLE_HEIGHT = 200;

// 직사각형과 선의 교차점 계산
const getRectIntersection = (
  rectX: number,
  rectY: number,
  rectWidth: number,
  rectHeight: number,
  lineStartX: number,
  lineStartY: number,
  lineEndX: number,
  lineEndY: number,
): { x: number; y: number } => {
  const centerX = rectX + rectWidth / 2;
  const centerY = rectY + rectHeight / 2;

  const dx = lineEndX - lineStartX;
  const dy = lineEndY - lineStartY;
  const angle = Math.atan2(dy, dx);

  const halfWidth = rectWidth / 2;
  const halfHeight = rectHeight / 2;
  const tan = Math.abs(Math.tan(angle));

  let intersectX = centerX;
  let intersectY = centerY;

  if (tan <= halfHeight / halfWidth) {
    if (angle >= -Math.PI / 2 && angle <= Math.PI / 2) {
      intersectX = rectX + rectWidth;
      intersectY = centerY + halfWidth * tan * Math.sign(angle);
    } else {
      intersectX = rectX;
      intersectY = centerY - halfWidth * tan * Math.sign(angle);
    }
  } else {
    if (angle >= 0) {
      intersectX = centerX + halfHeight / tan;
      intersectY = rectY + rectHeight;
    } else {
      intersectX = centerX - halfHeight / tan;
      intersectY = rectY;
    }
  }

  return { x: intersectX, y: intersectY };
};

// Cardinality에 따른 오른쪽 아이콘 결정
const getEndIconSrc = (cardinality: string): string => {
  switch (cardinality) {
    case '1':
      return relationOne;
    case '11':
      return relationOneOnly;
    case '13':
      return relationOneOrMany;
    case '01':
      return relationZeroOrOne;
    case '01-1':
      return relationZeroOrMany;
    case '013':
      return relationZeroOrOneOrMany;
    default:
      return relationOne;
  }
};

const RelationLine: React.FC<RelationLineProps> = ({
  relation,
  tables,
  onDelete,
  onHoverChange,
}) => {
  const [isHovered, setIsHovered] = useState(false);

  const sourceTable = tables.find((t) => t.id === relation.sourceTableId);
  const targetTable = tables.find((t) => t.id === relation.targetTableId);

  if (!sourceTable || !targetTable) {
    return null;
  }

  // 테이블 중심점
  const sourceCenterX = sourceTable.x + TABLE_WIDTH / 2;
  const sourceCenterY = sourceTable.y + TABLE_HEIGHT / 2;
  const targetCenterX = targetTable.x + TABLE_WIDTH / 2;
  const targetCenterY = targetTable.y + TABLE_HEIGHT / 2;

  // Source 테이블과 선의 교차점
  const sourceIntersection = getRectIntersection(
    sourceTable.x,
    sourceTable.y,
    TABLE_WIDTH,
    TABLE_HEIGHT,
    sourceCenterX,
    sourceCenterY,
    targetCenterX,
    targetCenterY,
  );

  // Target 테이블과 선의 교차점
  const targetIntersection = getRectIntersection(
    targetTable.x,
    targetTable.y,
    TABLE_WIDTH,
    TABLE_HEIGHT,
    targetCenterX,
    targetCenterY,
    sourceCenterX,
    sourceCenterY,
  );

  const screenSourceX = sourceIntersection.x;
  const screenSourceY = sourceIntersection.y;
  const screenTargetX = targetIntersection.x;
  const screenTargetY = targetIntersection.y;

  // 선의 스타일 결정 (식별: 실선, 비식별: 점선)
  const strokeDasharray = relation.type === 'identifying' ? '0' : '5,10';

  // 중간점 계산 (삭제 버튼 표시용)
  const midX = (screenSourceX + screenTargetX) / 2;
  const midY = (screenSourceY + screenTargetY) / 2;

  // 선의 각도 계산 (아이콘 회전용)
  const dx = screenTargetX - screenSourceX;
  const dy = screenTargetY - screenSourceY;
  const length = Math.sqrt(dx * dx + dy * dy);
  const angleStart = Math.atan2(dy, dx) * (180 / Math.PI);
  const angleEnd = angleStart; // 오른쪽도 같은 방향으로 회전

  // 아이콘 크기
  const iconSize = 24;
  const iconOffset = iconSize; // 아이콘 전체 크기만큼 오프셋 (테이블과 겹치지 않도록)

  // 선의 시작/끝점을 아이콘 크기만큼 안쪽으로 조정
  const unitDx = dx / length;
  const unitDy = dy / length;

  const adjustedSourceX = screenSourceX + unitDx * iconOffset;
  const adjustedSourceY = screenSourceY + unitDy * iconOffset;
  const adjustedTargetX = screenTargetX - unitDx * iconOffset;
  const adjustedTargetY = screenTargetY - unitDy * iconOffset;

  // 오른쪽 아이콘 소스
  const endIconSrc = getEndIconSrc(relation.cardinality);

  return (
    <>
      <g
        onMouseEnter={() => {
          setIsHovered(true);
          onHoverChange?.(relation.id);
        }}
        onMouseLeave={() => {
          setIsHovered(false);
          onHoverChange?.(null);
        }}
      >
        {/* 투명한 굵은 선 (호버 감지용) */}
        <line
          x1={adjustedSourceX}
          y1={adjustedSourceY}
          x2={adjustedTargetX}
          y2={adjustedTargetY}
          stroke="transparent"
          strokeWidth="20"
          style={{ cursor: 'pointer' }}
        />

        {/* 실제 관계선 */}
        <line
          x1={adjustedSourceX}
          y1={adjustedSourceY}
          x2={adjustedTargetX}
          y2={adjustedTargetY}
          stroke="#64748b"
          strokeWidth="2"
          strokeDasharray={strokeDasharray}
        />

        {/* 왼쪽 아이콘 (항상 relation-one.svg) */}
        <image
          href={relationOne}
          x={screenSourceX - iconSize / 2}
          y={screenSourceY - iconSize / 2}
          width={iconSize}
          height={iconSize}
          transform={`rotate(${angleStart}, ${screenSourceX}, ${screenSourceY})`}
        />

        {/* 오른쪽 아이콘 (cardinality에 따라 다름) */}
        <image
          href={endIconSrc}
          x={screenTargetX - iconSize / 2}
          y={screenTargetY - iconSize / 2}
          width={iconSize}
          height={iconSize}
          transform={`rotate(${angleEnd}, ${screenTargetX}, ${screenTargetY})`}
        />

        {/* 삭제 버튼 (호버 시에만 표시) */}
        {isHovered && onDelete && (
          <g
            onClick={(e) => {
              e.stopPropagation();
              onDelete(relation.id);
            }}
            style={{ cursor: 'pointer' }}
          >
            <circle cx={midX} cy={midY} r="12" fill="#ef4444" />
            <text
              x={midX}
              y={midY + 5}
              fill="white"
              fontSize="14"
              fontWeight="bold"
              textAnchor="middle"
              style={{ pointerEvents: 'none' }}
            >
              ×
            </text>
          </g>
        )}
      </g>
    </>
  );
};

export default RelationLine;
