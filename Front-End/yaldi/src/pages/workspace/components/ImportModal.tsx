import React, { useEffect, useState } from 'react';
import ModalLarge from '../../../components/common/ModalLarge';
import FilledButton from '../../../components/common/FilledButton';

interface ImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit?: (ddl: string) => void;
  isProcessing?: boolean;
}

const ImportModal: React.FC<ImportModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  isProcessing = false,
}) => {
  const [ddl, setDdl] = useState('');

  useEffect(() => {
    if (!isOpen) {
      setDdl('');
    }
  }, [isOpen]);

  const handleSubmit = () => {
    if (onSubmit) {
      onSubmit(ddl);
    } else {
      console.log('Import DDL 제출', ddl);
    }
  };

  const isDisabled = isProcessing || ddl.trim().length === 0;

  return (
    <ModalLarge isOpen={isOpen} onClose={onClose} title="Import">
      <div className="flex w-full flex-col gap-6 py-2">
        <textarea
          value={ddl}
          onChange={(event) => setDdl(event.target.value)}
          placeholder="SQL문(DDL)을 입력하면 AI 구문 오류 검토 후 entity를 생성할 수 있습니다."
          disabled={isProcessing}
          className="h-72 w-full resize-none rounded-[15px] border border-my-border bg-my-white p-4 text-base text-my-black placeholder:text-my-black/40 focus:outline-none focus:ring-2 focus:ring-blue disabled:bg-my-border/10 disabled:text-my-black/60"
        />

        <div className="flex justify-end">
          <FilledButton
            label={isProcessing ? '분석 중...' : '생성하기'}
            onClick={handleSubmit}
            disabled={isDisabled}
          />
        </div>
      </div>
    </ModalLarge>
  );
};

export default ImportModal;
