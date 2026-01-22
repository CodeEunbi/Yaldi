import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { createPortal } from 'react-dom';
import CircleCheckIcon from '../../../assets/icons/circle-check-icon.svg?react';
import CloseIcon from '../../../assets/icons/close_icon.svg?react';
import MoreIcon from '../../../assets/icons/more_icon.svg?react';
import SendIcon from '../../../assets/icons/send-icon.svg?react';
import UnsendIcon from '../../../assets/icons/unsend-icon.svg?react';
import type {
  TableActionButton,
  TableComment,
  TableReply,
} from '../../../types/tableCard';

type TableCardCommentPanelProps = {
  tableId: string;
  memoAction?: TableActionButton;
  isSelected: boolean;
  isEditingDisabled: boolean;
  isCommentOpen: boolean;
  comments: TableComment[];
  isCommentResolved: boolean;
  memoButtonRef: React.RefObject<HTMLButtonElement | null>;
  onToggleComments?: (tableId: string) => void;
  onCloseComments?: () => void;
  onToggleCommentResolved?: (tableId: string) => void;
  onSubmitComment?: (tableId: string, content: string) => void;
  onUpdateComment?: (
    tableId: string,
    commentId: string,
    content: string,
  ) => void;
  onDeleteComment?: (tableId: string, commentId: string) => void;
  onSubmitReply?: (tableId: string, commentId: string, content: string) => void;
  onUpdateReply?: (
    tableId: string,
    commentId: string,
    replyId: string,
    content: string,
  ) => void;
  onDeleteReply?: (tableId: string, commentId: string, replyId: string) => void;
};

const TableCardCommentPanel: React.FC<TableCardCommentPanelProps> = ({
  tableId,
  memoAction,
  isSelected,
  isEditingDisabled,
  isCommentOpen,
  comments,
  isCommentResolved,
  memoButtonRef,
  onToggleComments,
  onCloseComments,
  onToggleCommentResolved,
  onSubmitComment,
  onUpdateComment,
  onDeleteComment,
  onSubmitReply,
  onUpdateReply,
  onDeleteReply,
}) => {
  const [commentValue, setCommentValue] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editingCommentValue, setEditingCommentValue] = useState('');
  const [activeCommentMenuId, setActiveCommentMenuId] = useState<string | null>(
    null,
  );
  const [commentMenuPosition, setCommentMenuPosition] = useState<{
    top: number;
    left: number;
  } | null>(null);
  const [expandedReplyCommentId, setExpandedReplyCommentId] = useState<
    string | null
  >(null);
  const [replyValues, setReplyValues] = useState<Record<string, string>>({});
  const [editingReplyTarget, setEditingReplyTarget] = useState<{
    commentId: string;
    replyId: string;
  } | null>(null);
  const [editingReplyValue, setEditingReplyValue] = useState('');

  const commentPanelRef = useRef<HTMLDivElement | null>(null);
  const commentMenuButtonRefs = useRef<
    Record<string, HTMLButtonElement | null>
  >({});
  const commentMenuPortalRef = useRef<HTMLDivElement | null>(null);
  const commentListRef = useRef<HTMLDivElement | null>(null);

  const closeCommentMenu = useCallback(() => {
    setActiveCommentMenuId(null);
    setCommentMenuPosition(null);
  }, []);

  const activeComment = useMemo(
    () =>
      comments.find((comment) => comment.id === activeCommentMenuId) ?? null,
    [activeCommentMenuId, comments],
  );

  useEffect(() => {
    if (!isCommentOpen) {
      setEditingCommentId(null);
      setEditingCommentValue('');
      closeCommentMenu();
      setExpandedReplyCommentId(null);
      setReplyValues({});
      setEditingReplyTarget(null);
      setEditingReplyValue('');
    }
  }, [closeCommentMenu, isCommentOpen]);

  useEffect(() => {
    if (!isCommentOpen) {
      return;
    }

    const handleDocumentMouseDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (commentPanelRef.current?.contains(target)) {
        return;
      }
      if (memoButtonRef.current?.contains(target)) {
        return;
      }
      onCloseComments?.();
    };

    document.addEventListener('mousedown', handleDocumentMouseDown);
    return () => {
      document.removeEventListener('mousedown', handleDocumentMouseDown);
    };
  }, [isCommentOpen, memoButtonRef, onCloseComments]);

  useEffect(() => {
    if (!isCommentOpen) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onCloseComments?.();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isCommentOpen, onCloseComments]);

  useEffect(() => {
    if (!activeCommentMenuId) {
      return;
    }
    const commentListElement = commentListRef.current;
    if (!commentListElement) {
      return;
    }

    const handleScroll = () => {
      closeCommentMenu();
    };

    commentListElement.addEventListener('scroll', handleScroll);
    return () => {
      commentListElement.removeEventListener('scroll', handleScroll);
    };
  }, [activeCommentMenuId, closeCommentMenu]);

  useEffect(() => {
    if (!activeCommentMenuId) {
      return;
    }

    const handleDocumentMouseDown = (event: MouseEvent) => {
      const target = event.target as Node;
      const menuElement = commentMenuPortalRef.current;
      const buttonElement = commentMenuButtonRefs.current[activeCommentMenuId];
      if (
        (menuElement && menuElement.contains(target)) ||
        (buttonElement && buttonElement.contains(target))
      ) {
        return;
      }
      closeCommentMenu();
    };

    const handleWindowResize = () => {
      closeCommentMenu();
    };

    document.addEventListener('mousedown', handleDocumentMouseDown);
    window.addEventListener('resize', handleWindowResize);
    return () => {
      document.removeEventListener('mousedown', handleDocumentMouseDown);
      window.removeEventListener('resize', handleWindowResize);
    };
  }, [activeCommentMenuId, closeCommentMenu]);

  useEffect(() => {
    if (!activeCommentMenuId) {
      commentMenuPortalRef.current = null;
    }
  }, [activeCommentMenuId]);

  const handleCommentSubmit = useCallback(() => {
    const trimmed = commentValue.trim();
    if (!trimmed) {
      return;
    }
    onSubmitComment?.(tableId, trimmed);
    setCommentValue('');
  }, [commentValue, onSubmitComment, tableId]);

  const handleCommentEditStart = useCallback(
    (comment: TableComment) => {
      if (isEditingDisabled) {
        return;
      }
      setEditingCommentId(comment.id);
      setEditingCommentValue(comment.content);
      closeCommentMenu();
    },
    [closeCommentMenu, isEditingDisabled],
  );

  const handleCommentEditCancel = useCallback(() => {
    setEditingCommentId(null);
    setEditingCommentValue('');
  }, []);

  const handleCommentEditSave = useCallback(() => {
    if (!editingCommentId) {
      return;
    }
    const trimmed = editingCommentValue.trim();
    if (!trimmed) {
      return;
    }
    onUpdateComment?.(tableId, editingCommentId, trimmed);
    setEditingCommentId(null);
    setEditingCommentValue('');
  }, [editingCommentId, editingCommentValue, onUpdateComment, tableId]);

  const handleCommentDelete = useCallback(
    (commentId: string) => {
      if (isEditingDisabled) {
        return;
      }
      closeCommentMenu();
      if (editingCommentId === commentId) {
        setEditingCommentId(null);
        setEditingCommentValue('');
      }
      onDeleteComment?.(tableId, commentId);
      setExpandedReplyCommentId((prev) => (prev === commentId ? null : prev));
      setReplyValues((prev) => {
        if (!(commentId in prev)) {
          return prev;
        }
        const next = { ...prev };
        delete next[commentId];
        return next;
      });
      if (editingReplyTarget?.commentId === commentId) {
        setEditingReplyTarget(null);
        setEditingReplyValue('');
      }
    },
    [
      closeCommentMenu,
      editingCommentId,
      editingReplyTarget?.commentId,
      isEditingDisabled,
      onDeleteComment,
      tableId,
    ],
  );

  const handleToggleReplySection = useCallback(
    (commentId: string) => {
      setExpandedReplyCommentId((prev) => {
        const isClosing = prev === commentId;
        if (isClosing) {
          if (editingReplyTarget?.commentId === commentId) {
            setEditingReplyTarget(null);
            setEditingReplyValue('');
          }
          return null;
        }
        return commentId;
      });
      setReplyValues((prev) => ({
        ...prev,
        [commentId]: prev[commentId] ?? '',
      }));
    },
    [editingReplyTarget?.commentId],
  );

  const handleReplyChange = useCallback((commentId: string, value: string) => {
    setReplyValues((prev) => ({
      ...prev,
      [commentId]: value,
    }));
  }, []);

  const handleReplySubmit = useCallback(
    (commentId: string) => {
      const trimmed = (replyValues[commentId] ?? '').trim();
      if (!trimmed) {
        return;
      }
      onSubmitReply?.(tableId, commentId, trimmed);
      setReplyValues((prev) => ({
        ...prev,
        [commentId]: '',
      }));
    },
    [onSubmitReply, replyValues, tableId],
  );

  const handleReplyEditStart = useCallback(
    (commentId: string, reply: TableReply) => {
      if (isEditingDisabled) {
        return;
      }
      setEditingReplyTarget({ commentId, replyId: reply.id });
      setEditingReplyValue(reply.content);
    },
    [isEditingDisabled],
  );

  const handleReplyEditCancel = useCallback(() => {
    setEditingReplyTarget(null);
    setEditingReplyValue('');
  }, []);

  const handleReplyEditSave = useCallback(() => {
    if (!editingReplyTarget) {
      return;
    }
    const trimmed = editingReplyValue.trim();
    if (!trimmed) {
      return;
    }
    onUpdateReply?.(
      tableId,
      editingReplyTarget.commentId,
      editingReplyTarget.replyId,
      trimmed,
    );
    setEditingReplyTarget(null);
    setEditingReplyValue('');
  }, [editingReplyTarget, editingReplyValue, onUpdateReply, tableId]);

  const handleReplyDelete = useCallback(
    (commentId: string, replyId: string) => {
      if (isEditingDisabled) {
        return;
      }
      if (
        editingReplyTarget &&
        editingReplyTarget.commentId === commentId &&
        editingReplyTarget.replyId === replyId
      ) {
        setEditingReplyTarget(null);
        setEditingReplyValue('');
      }
      onDeleteReply?.(tableId, commentId, replyId);
    },
    [editingReplyTarget, isEditingDisabled, onDeleteReply, tableId],
  );

  const handleCommentAreaClick = useCallback(
    (event: React.MouseEvent<HTMLElement>, commentId: string) => {
      if (
        (event.target as HTMLElement).closest('[data-comment-action="true"]')
      ) {
        return;
      }
      if (editingCommentId && editingCommentId !== commentId) {
        setEditingCommentId(null);
        setEditingCommentValue('');
      }
      closeCommentMenu();
      handleToggleReplySection(commentId);
    },
    [closeCommentMenu, editingCommentId, handleToggleReplySection],
  );

  const commentActionMenu =
    activeComment && commentMenuPosition
      ? createPortal(
          <div
            ref={(node) => {
              commentMenuPortalRef.current = node;
            }}
            onMouseDown={(event) => event.stopPropagation()}
            className="z-[1200] w-28 overflow-hidden rounded-md border border-my-border/60 bg-white shadow-[0_12px_24px_-12px_rgba(15,23,42,0.32)]"
            style={{
              position: 'fixed',
              top: commentMenuPosition.top,
              left: commentMenuPosition.left,
            }}
          >
            <button
              type="button"
              onClick={() => handleCommentEditStart(activeComment)}
              className="flex w-full items-center px-3 py-2 text-left text-xs text-my-black transition-colors hover:bg-gray-100"
            >
              수정
            </button>
            <button
              type="button"
              onClick={() => handleCommentDelete(activeComment.id)}
              className="flex w-full items-center px-3 py-2 text-left text-xs text-red-500 transition-colors hover:bg-red-50"
            >
              삭제
            </button>
          </div>,
          document.body,
        )
      : null;

  const isCommentReady = commentValue.trim().length > 0;
  const shouldEnableCommentScroll = comments.length > 1;

  if (!isSelected || !memoAction) {
    return null;
  }

  const handleMemoButtonClick = (
    event: React.MouseEvent<HTMLButtonElement>,
  ) => {
    event.stopPropagation();
    if (isEditingDisabled) {
      return;
    }
    if (onToggleComments) {
      onToggleComments(tableId);
      return;
    }
    memoAction.onClick?.(tableId);
  };

  return (
    <div className="absolute -top-10 right-2 z-40">
      <div className="relative">
        <button
          ref={memoButtonRef}
          type="button"
          disabled={isEditingDisabled}
          onClick={handleMemoButtonClick}
          className={`flex h-8 w-8 items-center justify-center rounded-md shadow ${
            memoAction.bgClass
          } ${isEditingDisabled ? 'cursor-not-allowed' : ''} ${
            isCommentOpen ? 'ring-2 ring-blue' : ''
          }`}
          aria-label={memoAction.label}
          title={memoAction.label}
          aria-pressed={isCommentOpen}
        >
          <memoAction.Icon className="h-[20px] w-[20px]" />
        </button>
        {isCommentOpen ? (
          <div
            ref={commentPanelRef}
            className="absolute top-0 left-full z-50 ml-3 w-80 rounded-[20px] bg-white shadow-[0_20px_40px_-20px_rgba(15,23,42,0.18)]"
          >
            <div className="flex items-center justify-between border-b border-my-border/50 px-4 py-3">
              <h3 className="text-base font-semibold text-my-black">댓글</h3>
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    onToggleCommentResolved?.(tableId);
                  }}
                  className="flex h-8 w-8 items-center justify-center rounded-full text-my-gray-500 transition-colors hover:bg-red-50 hover:text-red-500"
                  aria-label="해결 완료"
                  aria-pressed={isCommentResolved}
                >
                  <CircleCheckIcon className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    onCloseComments?.();
                  }}
                  className="flex h-8 w-8 items-center justify-center rounded-full text-my-gray-500 transition-colors hover:bg-gray-100 hover:text-my-black"
                  aria-label="닫기"
                >
                  <CloseIcon className="h-4 w-4" />
                </button>
              </div>
            </div>
            <div
              ref={commentListRef}
              className={`flex max-h-72 flex-col gap-6 px-4 py-4 ${
                shouldEnableCommentScroll
                  ? 'overflow-y-auto pr-1'
                  : 'overflow-visible'
              }`}
            >
              {comments.length === 0 ? (
                <p className="text-sm text-my-gray-500">
                  첫 댓글을 남겨보세요.
                </p>
              ) : (
                comments.map((comment) => {
                  const isReplySectionOpen =
                    expandedReplyCommentId === comment.id;
                  const replyValue = replyValues[comment.id] ?? '';
                  const replies = comment.replies ?? [];

                  return (
                    <article
                      key={comment.id}
                      className="flex flex-col gap-2"
                      onClick={(event) =>
                        handleCommentAreaClick(event, comment.id)
                      }
                    >
                      <header className="relative flex items-center justify-between">
                        <div className="flex items-baseline gap-2">
                          <span className="text-sm font-semibold text-my-black">
                            {comment.author}
                          </span>
                          <span className="text-xs text-my-gray-500">
                            {comment.createdAt}
                          </span>
                        </div>
                        <button
                          ref={(node) => {
                            commentMenuButtonRefs.current[comment.id] = node;
                          }}
                          type="button"
                          disabled={isEditingDisabled}
                          onClick={(event) => {
                            event.stopPropagation();
                            if (isEditingDisabled) {
                              return;
                            }
                            const buttonElement =
                              commentMenuButtonRefs.current[comment.id];
                            if (!buttonElement) {
                              return;
                            }
                            if (activeCommentMenuId === comment.id) {
                              closeCommentMenu();
                              return;
                            }
                            const rect = buttonElement.getBoundingClientRect();
                            const menuWidth = 112;
                            const menuHeight = 72;
                            const spacing = 6;

                            let left = rect.right - menuWidth;
                            const viewportWidth = window.innerWidth;
                            left = Math.min(
                              left,
                              viewportWidth - menuWidth - 8,
                            );
                            left = Math.max(left, 8);

                            let top = rect.bottom + spacing;
                            const viewportHeight = window.innerHeight;
                            if (top + menuHeight > viewportHeight - 8) {
                              top = Math.max(
                                rect.top - spacing - menuHeight,
                                8,
                              );
                            }

                            setCommentMenuPosition({ top, left });
                            setActiveCommentMenuId(comment.id);
                          }}
                          className={`flex h-6 w-6 items-center justify-center rounded-md text-my-gray-400 transition-colors ${
                            isEditingDisabled
                              ? 'cursor-not-allowed opacity-40'
                              : 'hover:bg-gray-100 hover:text-my-black'
                          }`}
                          aria-label="댓글 메뉴"
                          data-comment-action="true"
                        >
                          <MoreIcon className="h-3.5 w-3.5" />
                        </button>
                      </header>
                      {editingCommentId === comment.id ? (
                        <div className="flex flex-col gap-2">
                          <textarea
                            value={editingCommentValue}
                            onChange={(event) =>
                              setEditingCommentValue(event.target.value)
                            }
                            rows={3}
                            className="w-full resize-none rounded-lg border border-my-border/80 bg-white px-3 py-2 text-sm text-my-black outline-none focus:border-blue"
                            data-comment-action="true"
                          />
                          <div className="flex justify-end gap-2">
                            <button
                              type="button"
                              onClick={handleCommentEditCancel}
                              className="rounded-full px-3 py-1.5 text-xs font-medium text-my-gray-500 transition-colors hover:bg-gray-100 hover:text-my-black"
                              data-comment-action="true"
                            >
                              취소
                            </button>
                            <button
                              type="button"
                              onClick={handleCommentEditSave}
                              disabled={!editingCommentValue.trim()}
                              className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
                                editingCommentValue.trim()
                                  ? 'bg-blue text-white hover:bg-blue/90'
                                  : 'bg-my-gray-200 text-my-gray-400'
                              }`}
                              data-comment-action="true"
                            >
                              저장
                            </button>
                          </div>
                        </div>
                      ) : (
                        <p className="whitespace-pre-wrap text-sm text-my-black">
                          {comment.content}
                        </p>
                      )}
                      <button
                        type="button"
                        onClick={(event) => {
                          event.stopPropagation();
                          handleToggleReplySection(comment.id);
                        }}
                        className="flex w-fit items-center gap-1 text-xs font-medium text-my-gray-500 transition-colors hover:text-my-black"
                        data-comment-action="true"
                      >
                        {isReplySectionOpen ? '답글 숨기기' : '답글 달기'}
                        {replies.length > 0 ? (
                          <span className="text-[10px] font-semibold text-blue">
                            {replies.length}
                          </span>
                        ) : null}
                      </button>
                      {isReplySectionOpen ? (
                        <div
                          className="ml-4 flex flex-col gap-3 border-l border-my-border/40 pl-3 pt-1"
                          data-comment-action="true"
                        >
                          {replies.length > 0 ? (
                            replies.map((reply) => {
                              const isEditingThisReply =
                                editingReplyTarget?.commentId === comment.id &&
                                editingReplyTarget.replyId === reply.id;
                              return (
                                <div
                                  key={reply.id}
                                  className="flex flex-col gap-1 rounded-md bg-white/60 px-2 py-1"
                                  data-comment-action="true"
                                >
                                  <div className="flex items-center justify-between gap-3">
                                    <div className="flex items-baseline gap-2">
                                      <span className="text-xs font-semibold text-my-black">
                                        {reply.author}
                                      </span>
                                      <span className="text-[11px] text-my-gray-500">
                                        {reply.createdAt}
                                      </span>
                                    </div>
                                    <div className="flex items-center gap-1">
                                      {isEditingThisReply ? null : (
                                        <>
                                          <button
                                            type="button"
                                            onClick={(event) => {
                                              event.stopPropagation();
                                              handleReplyEditStart(
                                                comment.id,
                                                reply,
                                              );
                                            }}
                                            className="rounded-full px-2 py-1 text-[11px] font-medium text-my-gray-500 transition-colors hover:bg-gray-100 hover:text-my-black"
                                            data-comment-action="true"
                                          >
                                            수정
                                          </button>
                                          <button
                                            type="button"
                                            onClick={(event) => {
                                              event.stopPropagation();
                                              handleReplyDelete(
                                                comment.id,
                                                reply.id,
                                              );
                                            }}
                                            className="rounded-full px-2 py-1 text-[11px] font-medium text-red-500 transition-colors hover:bg-red-50"
                                            data-comment-action="true"
                                          >
                                            삭제
                                          </button>
                                        </>
                                      )}
                                    </div>
                                  </div>
                                  {isEditingThisReply ? (
                                    <div className="flex flex-col gap-2">
                                      <textarea
                                        value={editingReplyValue}
                                        onChange={(event) =>
                                          setEditingReplyValue(
                                            event.target.value,
                                          )
                                        }
                                        rows={2}
                                        className="w-full resize-none rounded-md border border-my-border/70 bg-white px-2 py-1 text-xs text-my-black outline-none focus:border-blue"
                                        data-comment-action="true"
                                      />
                                      <div className="flex justify-end gap-1">
                                        <button
                                          type="button"
                                          onClick={(event) => {
                                            event.stopPropagation();
                                            handleReplyEditCancel();
                                          }}
                                          className="rounded-full px-2 py-1 text-[11px] font-medium text-my-gray-500 transition-colors hover:bg-gray-100 hover:text-my-black"
                                          data-comment-action="true"
                                        >
                                          취소
                                        </button>
                                        <button
                                          type="button"
                                          disabled={!editingReplyValue.trim()}
                                          onClick={(event) => {
                                            event.stopPropagation();
                                            handleReplyEditSave();
                                          }}
                                          className={`rounded-full px-2 py-1 text-[11px] font-medium transition-colors ${
                                            editingReplyValue.trim()
                                              ? 'bg-blue text-white hover:bg-blue/90'
                                              : 'bg-my-gray-200 text-my-gray-400'
                                          }`}
                                          data-comment-action="true"
                                        >
                                          저장
                                        </button>
                                      </div>
                                    </div>
                                  ) : (
                                    <p className="whitespace-pre-wrap text-xs text-my-black">
                                      {reply.content}
                                    </p>
                                  )}
                                </div>
                              );
                            })
                          ) : (
                            <p className="text-[11px] text-my-gray-400">
                              아직 등록된 답글이 없습니다.
                            </p>
                          )}
                          <div className="flex items-center gap-2 rounded-xl bg-[#f5f6f8] px-3 py-1.5">
                            <input
                              value={replyValue}
                              onChange={(event) =>
                                handleReplyChange(
                                  comment.id,
                                  event.target.value,
                                )
                              }
                              onKeyDown={(event) => {
                                if (event.key === 'Enter' && !event.shiftKey) {
                                  event.preventDefault();
                                  event.stopPropagation();
                                  handleReplySubmit(comment.id);
                                }
                              }}
                              placeholder="답글 작성"
                              className="flex-1 bg-transparent text-xs text-my-black placeholder:text-my-gray-400 focus:outline-none"
                            />
                            <button
                              type="button"
                              disabled={!replyValue.trim()}
                              onClick={(event) => {
                                event.stopPropagation();
                                handleReplySubmit(comment.id);
                              }}
                              className={`text-xs font-semibold transition-colors ${
                                replyValue.trim()
                                  ? 'text-blue hover:text-blue/80'
                                  : 'text-my-gray-400'
                              }`}
                            >
                              등록
                            </button>
                          </div>
                        </div>
                      ) : null}
                    </article>
                  );
                })
              )}
            </div>
            <div className="border-t border-my-border/50 px-4 py-3.5">
              <div className="flex items-center gap-2.5 rounded-2xl bg-[#f5f6f8] px-3.5 py-2">
                <input
                  value={commentValue}
                  onChange={(event) => setCommentValue(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' && !event.shiftKey) {
                      event.preventDefault();
                      handleCommentSubmit();
                    }
                  }}
                  className="flex-1 bg-transparent text-sm text-my-black placeholder:text-my-gray-400 focus:outline-none"
                  placeholder="Reply"
                />
                <button
                  type="button"
                  onClick={handleCommentSubmit}
                  className="flex h-9 w-9 items-center justify-center disabled:cursor-not-allowed"
                  disabled={!isCommentReady}
                  aria-label="댓글 등록"
                >
                  {isCommentReady ? (
                    <SendIcon className="h-[30px] w-[30px]" />
                  ) : (
                    <UnsendIcon className="h-[30px] w-[30px]" />
                  )}
                </button>
              </div>
            </div>
            {commentActionMenu}
          </div>
        ) : null}
      </div>
    </div>
  );
};

export default React.memo(TableCardCommentPanel);
