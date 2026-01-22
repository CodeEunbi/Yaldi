import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { createPortal } from 'react-dom';
import MiniMapIcon from '../../../assets/icons/minimap-icon.svg?react';
import { useWorkspace } from '../WorkSpace';

interface MiniMapProps {
  onClick?: () => void;
}

const MiniMap: React.FC<MiniMapProps> = ({ onClick }) => {
  const handleClick = () => {
    if (onClick) {
      onClick();
    } else {
      console.log('minimap 아이콘 클릭');
    }
  };

  return (
    <button
      data-minimap-exclude="true"
      onClick={handleClick}
      className="w-8 h-8 flex items-center justify-center mb-2 hover:bg-ai-chat rounded transition-colors"
      aria-label="미니맵"
    >
      <MiniMapIcon className="w-5 h-5 text-my-black" />
    </button>
  );
};

export default MiniMap;

type MiniMapEntity = {
  id: string;
  width: number;
  height: number;
  top: number;
  left: number;
};

const MINIMAP_WIDTH = 240;
const MINIMAP_HEIGHT = 160;
const SIDEBAR_WIDTH = 48; // w-12 = 3rem = 48px
const SIDEBAR_GUTTER = 24; // 여유 공간
const CONTENT_PADDING = 16;
const MIN_ENTITY_SIZE = 6;
const CANVAS_SIZE = 5000;

const clampViewportValue = (value: number, viewportSize: number) => {
  if (viewportSize >= CANVAS_SIZE) {
    return (CANVAS_SIZE - viewportSize) / 2;
  }
  return Math.min(Math.max(value, 0), CANVAS_SIZE - viewportSize);
};

interface WorkspaceSnapshot {
  workspaceRect: {
    width: number;
    height: number;
  };
  tableRects: MiniMapEntity[];
  workspaceHTML: string;
  workspaceClassName: string;
}

const getWorkspaceElement = (): HTMLElement | null => {
  if (typeof document === 'undefined') {
    return null;
  }
  return document.querySelector<HTMLElement>('[data-workspace-canvas="true"]');
};

interface MiniMapOverlayProps {
  isOpen: boolean;
}

export const MiniMapOverlay: React.FC<MiniMapOverlayProps> = ({ isOpen }) => {
  const { tables, zoom, pan, setPan } = useWorkspace();
  const [snapshot, setSnapshot] = useState<WorkspaceSnapshot | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const draggingPointerIdRef = useRef<number | null>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const workspaceElement = getWorkspaceElement();

    if (!workspaceElement) {
      setSnapshot(null);
      return;
    }

    let animationFrame: number | null = null;

    const collectSnapshot = () => {
      if (!workspaceElement.isConnected) {
        return;
      }

      const workspaceRect = workspaceElement.getBoundingClientRect();
      const workspaceClone = workspaceElement.cloneNode(true) as HTMLElement;
      workspaceClone
        .querySelectorAll<HTMLElement>('[data-minimap-exclude="true"]')
        .forEach((element) => element.remove());
      workspaceClone.classList.remove('flex-1');
      workspaceClone.style.removeProperty('flex');
      const tableElements = Array.from(
        workspaceElement.querySelectorAll<HTMLElement>('[data-table-id]'),
      );

      const invZoom = 1 / Math.max(zoom, 0.0001);

      const tableRects: MiniMapEntity[] = tableElements.map((element) => {
        const rect = element.getBoundingClientRect();
        const worldLeft = (rect.left - workspaceRect.left - pan.x) * invZoom;
        const worldTop = (rect.top - workspaceRect.top - pan.y) * invZoom;
        const worldWidth = rect.width * invZoom;
        const worldHeight = rect.height * invZoom;

        return {
          id: element.dataset.tableId ?? crypto.randomUUID(),
          width: worldWidth,
          height: worldHeight,
          top: worldTop,
          left: worldLeft,
        };
      });

      setSnapshot({
        workspaceRect: {
          width: workspaceRect.width,
          height: workspaceRect.height,
        },
        tableRects,
        workspaceHTML: workspaceClone.innerHTML,
        workspaceClassName: workspaceClone.className,
      });
    };

    const scheduleCollect = () => {
      if (animationFrame !== null) {
        return;
      }
      animationFrame = window.requestAnimationFrame(() => {
        collectSnapshot();
        animationFrame = null;
      });
    };

    const resizeObserver =
      typeof ResizeObserver !== 'undefined'
        ? new ResizeObserver(() => scheduleCollect())
        : null;

    if (resizeObserver) {
      resizeObserver.observe(workspaceElement);
    }

    const observeTables = () => {
      if (!resizeObserver) {
        return;
      }
      const tableElements = Array.from(
        workspaceElement.querySelectorAll<HTMLElement>('[data-table-id]'),
      );
      tableElements.forEach((element) => resizeObserver.observe(element));
    };

    observeTables();

    const mutationObserver =
      typeof MutationObserver !== 'undefined'
        ? new MutationObserver(() => {
            observeTables();
            scheduleCollect();
          })
        : null;

    mutationObserver?.observe(workspaceElement, {
      childList: true,
      subtree: true,
    });

    window.addEventListener('resize', scheduleCollect);
    window.addEventListener('scroll', scheduleCollect, true);
    scheduleCollect();

    return () => {
      if (animationFrame !== null) {
        window.cancelAnimationFrame(animationFrame);
      }
      resizeObserver?.disconnect();
      mutationObserver?.disconnect();
      window.removeEventListener('resize', scheduleCollect);
      window.removeEventListener('scroll', scheduleCollect, true);
    };
  }, [isOpen, pan.x, pan.y, tables, zoom]);

  const minimapData = useMemo(() => {
    if (!snapshot) {
      return {
        tables: [] as MiniMapEntity[],
        viewport: null as MiniMapEntity | null,
        isEmpty: true,
        bounds: { minX: 0, minY: 0, maxX: 0, maxY: 0 },
        scale: 1,
        offsetX: 0,
        offsetY: 0,
      };
    }

    const { workspaceRect, tableRects } = snapshot;

    const minX = 0;
    const minY = 0;
    const maxX = CANVAS_SIZE;
    const maxY = CANVAS_SIZE;

    const contentWidth = Math.max(maxX - minX, 1);
    const contentHeight = Math.max(maxY - minY, 1);
    const availableWidth = MINIMAP_WIDTH - CONTENT_PADDING * 2;
    const availableHeight = MINIMAP_HEIGHT - CONTENT_PADDING * 2;
    const scale = Math.min(
      availableWidth / contentWidth,
      availableHeight / contentHeight,
    );

    const offsetX = (MINIMAP_WIDTH - contentWidth * scale) / 2;
    const offsetY = (MINIMAP_HEIGHT - contentHeight * scale) / 2;

    const scaledTables = tableRects.map((rect) => ({
      id: rect.id,
      width: Math.max(rect.width * scale, MIN_ENTITY_SIZE),
      height: Math.max(rect.height * scale, MIN_ENTITY_SIZE),
      top: offsetY + (rect.top - minY) * scale,
      left: offsetX + (rect.left - minX) * scale,
    }));

    const invZoom = 1 / Math.max(zoom, 0.0001);
    const viewportWorldWidth = workspaceRect.width * invZoom;
    const viewportWorldHeight = workspaceRect.height * invZoom;
    const viewportWorldLeft = clampViewportValue(
      -pan.x * invZoom,
      viewportWorldWidth,
    );
    const viewportWorldTop = clampViewportValue(
      -pan.y * invZoom,
      viewportWorldHeight,
    );

    return {
      tables: scaledTables,
      viewport: {
        id: 'viewport',
        width: Math.max(viewportWorldWidth * scale, MIN_ENTITY_SIZE),
        height: Math.max(viewportWorldHeight * scale, MIN_ENTITY_SIZE),
        top: offsetY + (viewportWorldTop - minY) * scale,
        left: offsetX + (viewportWorldLeft - minX) * scale,
      },
      isEmpty: scaledTables.length === 0,
      bounds: { minX, minY, maxX, maxY },
      scale,
      offsetX,
      offsetY,
    };
  }, [pan.x, pan.y, snapshot, zoom]);

  const moveViewportTo = useCallback(
    (worldX: number, worldY: number) => {
      if (!snapshot) {
        return;
      }

      const invZoom = 1 / Math.max(zoom, 0.0001);
      const viewportWorldWidth = snapshot.workspaceRect.width * invZoom;
      const viewportWorldHeight = snapshot.workspaceRect.height * invZoom;

      const halfWidth = viewportWorldWidth / 2;
      const halfHeight = viewportWorldHeight / 2;

      const nextLeft = clampViewportValue(
        worldX - halfWidth,
        viewportWorldWidth,
      );
      const nextTop = clampViewportValue(
        worldY - halfHeight,
        viewportWorldHeight,
      );

      setPan({
        x: -nextLeft * zoom,
        y: -nextTop * zoom,
      });
    },
    [setPan, snapshot, zoom],
  );

  const getWorldPositionFromEvent = useCallback(
    (event: React.PointerEvent<HTMLDivElement>) => {
      if (!snapshot) {
        return null;
      }

      const container = containerRef.current;
      if (!container || minimapData.scale <= 0) {
        return null;
      }

      const rect = container.getBoundingClientRect();
      const localX = event.clientX - rect.left;
      const localY = event.clientY - rect.top;

      const worldX =
        minimapData.bounds.minX +
        (localX - minimapData.offsetX) / minimapData.scale;
      const worldY =
        minimapData.bounds.minY +
        (localY - minimapData.offsetY) / minimapData.scale;

      if (Number.isNaN(worldX) || Number.isNaN(worldY)) {
        return null;
      }

      const clampedX = Math.min(Math.max(worldX, 0), CANVAS_SIZE);
      const clampedY = Math.min(Math.max(worldY, 0), CANVAS_SIZE);

      return { worldX: clampedX, worldY: clampedY };
    },
    [minimapData, snapshot],
  );

  const handlePointerDown = useCallback(
    (event: React.PointerEvent<HTMLDivElement>) => {
      const position = getWorldPositionFromEvent(event);
      if (!position) {
        return;
      }

      event.preventDefault();
      const container = containerRef.current;
      if (!container) {
        return;
      }

      draggingPointerIdRef.current = event.pointerId;
      container.setPointerCapture(event.pointerId);
      moveViewportTo(position.worldX, position.worldY);
    },
    [getWorldPositionFromEvent, moveViewportTo],
  );

  const handlePointerMove = useCallback(
    (event: React.PointerEvent<HTMLDivElement>) => {
      if (draggingPointerIdRef.current !== event.pointerId) {
        return;
      }

      const position = getWorldPositionFromEvent(event);
      if (!position) {
        return;
      }

      moveViewportTo(position.worldX, position.worldY);
    },
    [getWorldPositionFromEvent, moveViewportTo],
  );

  const handlePointerUp = useCallback(
    (event: React.PointerEvent<HTMLDivElement>) => {
      if (draggingPointerIdRef.current !== event.pointerId) {
        return;
      }

      const container = containerRef.current;
      if (container) {
        container.releasePointerCapture(event.pointerId);
      }
      draggingPointerIdRef.current = null;
    },
    [],
  );

  if (!isOpen) return null;

  const overlay = (
    <div
      className="fixed z-30"
      style={{
        bottom: 24,
        right: SIDEBAR_WIDTH + SIDEBAR_GUTTER,
      }}
    >
      <div
        ref={containerRef}
        className="relative overflow-hidden border border-my-border bg-my-white shadow-md"
        style={{
          width: MINIMAP_WIDTH,
          height: MINIMAP_HEIGHT,
        }}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
      >
        {snapshot && (
          <div className="absolute inset-0 pointer-events-none">
            <div
              className="origin-top-left pointer-events-none"
              style={{
                width: snapshot.workspaceRect.width,
                height: snapshot.workspaceRect.height,
                transform: `translate(${minimapData.offsetX}px, ${
                  minimapData.offsetY
                }px) scale(${minimapData.scale}) translate(${-minimapData.bounds
                  .minX}px, ${-minimapData.bounds.minY}px)`,
              }}
            >
              <div
                className={snapshot.workspaceClassName}
                style={{
                  width: snapshot.workspaceRect.width,
                  height: snapshot.workspaceRect.height,
                  pointerEvents: 'none',
                }}
                dangerouslySetInnerHTML={{ __html: snapshot.workspaceHTML }}
              />
            </div>
          </div>
        )}

        {minimapData.viewport && (
          <div
            className="absolute border border-blue/60"
            style={{
              width: minimapData.viewport.width,
              height: minimapData.viewport.height,
              top: minimapData.viewport.top,
              left: minimapData.viewport.left,
            }}
          />
        )}
      </div>
    </div>
  );

  return createPortal(overlay, document.body);
};
