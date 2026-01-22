import React, { useEffect, useState } from 'react';
import ModalLarge from '../../../components/common/ModalLarge';
import FilledButton from '../../../components/common/FilledButton';

export type ExportDatabase = 'mysql' | 'postgresql';

interface ExportModalProps {
  isOpen: boolean;
  onClose: () => void;
  sqlPreview?: string;
  onClickPreview?: (database: ExportDatabase) => void;
  onClickDownload?: (database: ExportDatabase) => void;
  isPreviewLoading?: boolean;
  isDownloadLoading?: boolean;
}

const ExportModal: React.FC<ExportModalProps> = ({
  isOpen,
  onClose,
  sqlPreview,
  onClickPreview,
  onClickDownload,
  isPreviewLoading = false,
  isDownloadLoading = false,
}) => {
  const [selectedDb, setSelectedDb] = useState<ExportDatabase>('mysql');

  useEffect(() => {
    if (!isOpen) {
      setSelectedDb('mysql');
    }
  }, [isOpen]);

  const handlePreview = () => {
    if (onClickPreview) {
      onClickPreview(selectedDb);
    } else {
      console.log('SQL Preview 클릭', selectedDb);
    }
  };

  const handleDownload = () => {
    if (onClickDownload) {
      onClickDownload(selectedDb);
    } else {
      console.log('SQL Copy 클릭', selectedDb);
    }
  };

  return (
    <ModalLarge isOpen={isOpen} onClose={onClose} title="Export">
      <div className="flex w-full flex-col gap-6 py-2">
        <div className="flex items-center gap-6">
          <label className="flex items-center gap-2 text-base text-my-black">
            <input
              type="radio"
              name="export-database"
              value="mysql"
              checked={selectedDb === 'mysql'}
              onChange={() => setSelectedDb('mysql')}
              className="h-4 w-4 accent-blue"
            />
            MySQL
          </label>

          <label className="flex items-center gap-2 text-base text-my-black">
            <input
              type="radio"
              name="export-database"
              value="postgresql"
              checked={selectedDb === 'postgresql'}
              onChange={() => setSelectedDb('postgresql')}
              className="h-4 w-4 accent-blue"
            />
            PostgreSQL
          </label>
        </div>

        <textarea
          readOnly
          value={sqlPreview ?? ''}
          placeholder="선택한 데이터베이스 형식에 맞춰 생성된 SQL이 표시됩니다."
          className="h-72 w-full resize-none rounded-[15px] border border-my-border bg-my-white p-4 text-base text-my-black placeholder:text-my-black/40 focus:outline-none"
        />

        <div className="flex w-full justify-center gap-4">
          <FilledButton
            label={isPreviewLoading ? '생성 중...' : 'SQL Preview'}
            onClick={handlePreview}
            disabled={isPreviewLoading || isDownloadLoading}
            size="px-6 py-2.5"
          />
          <FilledButton
            label={isDownloadLoading ? '복사 중...' : 'SQL Copy'}
            onClick={handleDownload}
            disabled={isDownloadLoading}
            size="px-6 py-2.5"
          />
        </div>
      </div>
    </ModalLarge>
  );
};

export default ExportModal;
