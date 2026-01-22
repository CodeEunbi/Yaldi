import React, { useCallback, useMemo, useState } from 'react';
import SnapshotIcon from '../../../assets/icons/snapshot_icon.svg?react';
import RestoreIcon from '../../../assets/icons/restore-icon.svg?react';
import ZoomInIcon from '../../../assets/icons/zoom-in-icon.svg?react';
import DeleteIcon from '../../../assets/icons/delete_icon.svg?react';
import ChatBox from '../../../components/common/ChatBox';
import ModalSmall from '../../../components/common/ModalSmall';
import ModalLarge from '../../../components/common/ModalLarge';
import { useWorkspace } from '../WorkSpace';

interface SnapShotProps {
  isOpen: boolean;
  onToggle?: () => void;
  onClose: () => void;
  dimmed?: boolean;
}

const SnapShot: React.FC<SnapShotProps> = ({
  isOpen,
  onToggle,
  onClose,
  dimmed = false,
}) => {
  const { snapshots, createSnapshot, restoreSnapshot, deleteSnapshot } =
    useWorkspace();
  const [snapshotToDelete, setSnapshotToDelete] = useState<string | null>(null);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [previewSnapshotId, setPreviewSnapshotId] = useState<string | null>(
    null,
  );
  const snapshotToDeleteData = useMemo(
    () => snapshots.find((snapshot) => snapshot.id === snapshotToDelete),
    [snapshotToDelete, snapshots],
  );
  const previewSnapshot = useMemo(
    () =>
      snapshots.find((snapshot) => snapshot.id === previewSnapshotId) ?? null,
    [previewSnapshotId, snapshots],
  );

  const formatTimestamp = useCallback((timestamp: number) => {
    return new Date(timestamp).toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }, []);

  const handleClick = () => {
    onToggle?.();
  };

  const footer = (
    <button
      type="button"
      className="w-full h-10 bg-blue text-my-white rounded-lg font-pretendard text-sm font-semibold hover:bg-my-gray-800 transition-colors"
      onClick={() => createSnapshot()}
    >
      새 스냅샷 만들기
    </button>
  );

  const handleDeleteClick = (snapshotId: string) => {
    setSnapshotToDelete(snapshotId);
    setIsDeleteModalOpen(true);
  };

  const handlePreview = (snapshotId: string) => {
    setPreviewSnapshotId(snapshotId);
  };

  const handleCancelDelete = () => {
    setIsDeleteModalOpen(false);
    setSnapshotToDelete(null);
  };

  const handleConfirmDelete = () => {
    if (!snapshotToDeleteData) {
      return;
    }

    deleteSnapshot(snapshotToDeleteData.id);
    handleCancelDelete();
  };

  return (
    <>
      <button
        onClick={handleClick}
        className="w-8 h-8 flex items-center justify-center mb-2 hover:bg-ai-chat rounded transition-colors"
        aria-label="스냅샷"
      >
        <SnapshotIcon className="w-5 h-5 text-my-black" />
      </button>

      <ChatBox
        isOpen={isOpen}
        onClose={onClose}
        title="스냅샷"
        footer={footer}
        dimmed={dimmed}
      >
        <div className="flex flex-col gap-4">
          {snapshots.length > 0 ? (
            <ul className="flex flex-col gap-3">
              {snapshots.map((snapshot) => (
                <li
                  key={snapshot.id}
                  className="border border-my-border rounded-xl overflow-hidden"
                >
                  <div className="h-28 bg-my-gray-100 flex items-center justify-center text-xs text-my-gray-400 font-pretendard overflow-hidden">
                    {snapshot.previewDataUrl ? (
                      <img
                        src={snapshot.previewDataUrl}
                        alt={`${snapshot.name} 미리보기`}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <span>미리보기 생성 중...</span>
                    )}
                  </div>
                  <div className="p-3 flex flex-col gap-2">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1">
                        <p className="text-sm font-semibold text-my-black font-pretendard">
                          {snapshot.name}
                        </p>
                        <p className="text-xs text-my-gray-400 font-pretendard">
                          {snapshot.createdBy?.name ?? '알 수 없음'} ·{' '}
                          {formatTimestamp(snapshot.createdAt)}
                        </p>
                      </div>
                      <div className="flex items-center gap-1 text-my-gray-500">
                        <button
                          type="button"
                          className="w-8 h-8 flex items-center justify-center border border-my-border rounded-lg hover:bg-gray-50 transition-colors"
                          aria-label={`${snapshot.name} 롤백`}
                          onClick={() => restoreSnapshot(snapshot.id)}
                        >
                          <RestoreIcon className="w-4 h-4 text-my-black" />
                        </button>
                        <button
                          type="button"
                          className="w-8 h-8 flex items-center justify-center border border-my-border rounded-lg hover:bg-gray-50 transition-colors"
                          aria-label={`${snapshot.name} 확대`}
                          onClick={() => handlePreview(snapshot.id)}
                        >
                          <ZoomInIcon className="w-4 h-4 text-my-black" />
                        </button>
                        <button
                          type="button"
                          className="w-8 h-8 flex items-center justify-center border border-my-border rounded-lg hover:bg-gray-50 transition-colors text-my-red-500"
                          aria-label={`${snapshot.name} 삭제`}
                          onClick={() => handleDeleteClick(snapshot.id)}
                        >
                          <DeleteIcon className="w-4 h-4 text-my-red-500" />
                        </button>
                      </div>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-my-gray-400 font-pretendard">
              아직 저장된 스냅샷이 없습니다.
            </p>
          )}
        </div>
      </ChatBox>

      <ModalSmall
        isOpen={isDeleteModalOpen}
        onClose={handleCancelDelete}
        title="스냅샷 삭제"
      >
        <div className="flex flex-col gap-6">
          <p className="text-sm text-my-gray-600 font-pretendard leading-relaxed">
            {snapshotToDeleteData
              ? `'${snapshotToDeleteData.name}' 스냅샷을 삭제할까요? 삭제하면 되돌릴 수 없습니다.`
              : '선택된 스냅샷이 없습니다.'}
          </p>
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={handleCancelDelete}
              className="px-4 py-2 border border-my-border rounded-lg text-sm font-pretendard hover:bg-gray-50 transition-colors"
            >
              취소
            </button>
            <button
              type="button"
              onClick={handleConfirmDelete}
              className="px-4 py-2 rounded-lg text-sm font-pretendard font-semibold text-white bg-red-500 hover:bg-red-600 transition-colors"
            >
              삭제
            </button>
          </div>
        </div>
      </ModalSmall>

      <ModalLarge
        isOpen={Boolean(previewSnapshot)}
        onClose={() => setPreviewSnapshotId(null)}
        title={previewSnapshot?.name ?? '스냅샷 미리보기'}
      >
        <div className="flex flex-col gap-4">
          {previewSnapshot?.previewDataUrl ? (
            <div className="w-full max-h-[70vh] overflow-auto rounded-lg border border-my-border bg-my-gray-50">
              <img
                src={previewSnapshot.previewDataUrl}
                alt={`${previewSnapshot.name} 전체 미리보기`}
                className="w-full h-full object-contain"
              />
            </div>
          ) : (
            <p className="text-sm text-my-gray-500 font-pretendard">
              미리보기 이미지를 불러오는 중입니다...
            </p>
          )}
          {previewSnapshot ? (
            <div className="text-xs text-my-gray-400 font-pretendard">
              {previewSnapshot.createdBy?.name ?? '알 수 없음'} ·{' '}
              {formatTimestamp(previewSnapshot.createdAt)}
            </div>
          ) : null}
        </div>
      </ModalLarge>
    </>
  );
};

export default SnapShot;
