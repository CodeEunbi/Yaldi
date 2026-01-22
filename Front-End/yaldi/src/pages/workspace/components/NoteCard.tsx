import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import DeleteIcon from '../../../assets/icons/delete_icon.svg?react';
import PalatteIcon from '../../../assets/icons/palatte_icon.svg?react';
import type { WorkspaceNote } from '../WorkSpace';
import {
  DEFAULT_NOTE_COLOR,
  TABLE_COLOR_HEX_MAP,
  TABLE_COLOR_OPTIONS,
} from '../constants/colorOptions';

type NoteCardProps = {
  note: WorkspaceNote;
  onChangeContent?: (noteId: string, content: string) => void;
  onChangeColor?: (noteId: string, color: WorkspaceNote['color']) => void;
  onDelete?: (noteId: string) => void;
  autoFocus?: boolean;
};

const clampColorChannel = (value: number) => Math.max(0, Math.min(255, value));

const lightenHex = (hex: string, amount: number) => {
  const normalized = hex.replace('#', '');
  const red = parseInt(normalized.slice(0, 2), 16);
  const green = parseInt(normalized.slice(2, 4), 16);
  const blue = parseInt(normalized.slice(4, 6), 16);

  const lightenChannel = (channel: number) =>
    clampColorChannel(Math.round(channel + (255 - channel) * amount));

  const nextRed = lightenChannel(red);
  const nextGreen = lightenChannel(green);
  const nextBlue = lightenChannel(blue);

  return `#${nextRed.toString(16).padStart(2, '0')}${nextGreen
    .toString(16)
    .padStart(2, '0')}${nextBlue.toString(16).padStart(2, '0')}`;
};

const getRelativeLuminance = (hex: string) => {
  const normalized = hex.replace('#', '');
  const red = parseInt(normalized.slice(0, 2), 16);
  const green = parseInt(normalized.slice(2, 4), 16);
  const blue = parseInt(normalized.slice(4, 6), 16);

  const channelToLinear = (channel: number) => {
    const sRGB = channel / 255;
    if (sRGB <= 0.03928) {
      return sRGB / 12.92;
    }
    return ((sRGB + 0.055) / 1.055) ** 2.4;
  };

  const r = channelToLinear(red);
  const g = channelToLinear(green);
  const b = channelToLinear(blue);

  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

const getContrastingTextColor = (hex: string) =>
  getRelativeLuminance(hex) > 0.6 ? '#1c2a4a' : '#ffffff';

const NoteCard: React.FC<NoteCardProps> = ({
  note,
  onChangeContent,
  onChangeColor,
  onDelete,
  autoFocus = false,
}) => {
  const [value, setValue] = useState(note.content);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const paletteRef = useRef<HTMLDivElement | null>(null);
  const paletteButtonRef = useRef<HTMLButtonElement | null>(null);
  const [isPaletteOpen, setIsPaletteOpen] = useState(false);

  useEffect(() => {
    setValue(note.content);
  }, [note.content]);

  useEffect(() => {
    if (!autoFocus) {
      return;
    }
    const timer = window.setTimeout(() => {
      textareaRef.current?.focus();
      textareaRef.current?.select();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [autoFocus]);

  const handleChange = useCallback(
    (event: React.ChangeEvent<HTMLTextAreaElement>) => {
      const nextValue = event.target.value;
      setValue(nextValue);
      onChangeContent?.(note.id, nextValue);
    },
    [note.id, onChangeContent],
  );

  const handleDelete = useCallback(() => {
    onDelete?.(note.id);
  }, [note.id, onDelete]);

  useEffect(() => {
    if (!isPaletteOpen) {
      return;
    }

    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Node;
      if (paletteRef.current && paletteRef.current.contains(target)) {
        return;
      }
      if (
        paletteButtonRef.current &&
        paletteButtonRef.current.contains(target)
      ) {
        return;
      }
      setIsPaletteOpen(false);
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isPaletteOpen]);

  const activeColorKey = note.color ?? DEFAULT_NOTE_COLOR;
  const baseColorHex =
    TABLE_COLOR_HEX_MAP[activeColorKey] ??
    TABLE_COLOR_HEX_MAP[DEFAULT_NOTE_COLOR];
  const bodyColor = useMemo(
    () => lightenHex(baseColorHex, 0.35),
    [baseColorHex],
  );
  const headerTextColor = useMemo(
    () => getContrastingTextColor(baseColorHex),
    [baseColorHex],
  );

  return (
    <div
      data-note-id={note.id}
      className="absolute flex w-[200px] flex-col overflow-hidden rounded-lg shadow-md"
      style={{
        left: `${note.x}px`,
        top: `${note.y}px`,
        zIndex: 20,
        backgroundColor: bodyColor,
      }}
      onMouseDown={(event) => event.stopPropagation()}
      onClick={(event) => event.stopPropagation()}
    >
      <div
        className="relative flex items-center justify-between px-3 py-1 text-xs font-semibold"
        style={{
          backgroundColor: baseColorHex,
          color: headerTextColor,
        }}
      >
        <span>메모</span>
        {/* 뷰어 모드에서는 편집 버튼 숨김 */}
        {(onChangeColor || onDelete) && (
          <div className="flex items-center gap-1">
            {onChangeColor && (
              <button
                ref={paletteButtonRef}
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  setIsPaletteOpen((prev) => !prev);
                }}
                className="flex h-5 w-5 items-center justify-center rounded-full transition-colors hover:bg-black/10"
                aria-label="메모 색상 변경"
                title="메모 색상 변경"
                style={{ color: headerTextColor }}
              >
                <PalatteIcon className="h-3 w-3" />
              </button>
            )}
            {onDelete && (
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  setIsPaletteOpen(false);
                  handleDelete();
                }}
                className="flex h-5 w-5 items-center justify-center rounded-full transition-colors hover:bg-black/10"
                aria-label="메모 삭제"
                title="메모 삭제"
                style={{ color: headerTextColor }}
              >
                <DeleteIcon className="h-3 w-3" />
              </button>
            )}
          </div>
        )}
        {isPaletteOpen ? (
          <div
            ref={paletteRef}
            className="absolute right-0 top-9 grid grid-cols-4 gap-2 rounded-lg border border-my-border bg-white p-2 shadow-lg"
            onClick={(event) => event.stopPropagation()}
          >
            {TABLE_COLOR_OPTIONS.map(({ key, value }) => {
              const isActive = activeColorKey === value;
              return (
                <button
                  key={key}
                  type="button"
                  title={`${key} 색상 선택`}
                  aria-label={`${key} 색상 선택`}
                  onClick={(event) => {
                    event.stopPropagation();
                    onChangeColor?.(note.id, value);
                    setIsPaletteOpen(false);
                  }}
                  className={`h-6 w-6 rounded-md border-2 transition-transform duration-150 ${
                    isActive
                      ? 'scale-105 border-blue'
                      : 'border-transparent hover:scale-105'
                  } bg-${value}`}
                />
              );
            })}
          </div>
        ) : null}
      </div>
      <textarea
        ref={textareaRef}
        value={value}
        onChange={handleChange}
        rows={6}
        className="min-h-[120px] w-full resize-none bg-transparent px-3 pb-3 pt-2 text-sm outline-none"
        style={{
          color: '#1c2a4a',
        }}
        placeholder="메모를 입력하세요..."
        readOnly={!onChangeContent}
      />
    </div>
  );
};

export default NoteCard;
