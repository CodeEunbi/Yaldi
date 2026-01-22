import React from 'react';
import FilledButton from '../../../components/common/FilledButton';

interface ImportRetryModalProps {
  isOpen: boolean;
  message: string;
  onConfirm: () => void;
}

const ImportRetryModal: React.FC<ImportRetryModalProps> = ({
  isOpen,
  message,
  onConfirm,
}) => {
  if (!isOpen) {
    return null;
  }

  const handleOverlayClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (event.target === event.currentTarget) {
      onConfirm();
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm"
      onClick={handleOverlayClick}
    >
      <div className="flex w-[260px] flex-col items-center gap-6 rounded-3xl border border-my-border bg-my-white px-6 py-8 text-center shadow-2xl">
        <p className="text-sm font-medium text-my-black">{message}</p>
        <FilledButton
          label="확인"
          onClick={onConfirm}
          size="px-6 py-2 text-sm"
        />
      </div>
    </div>
  );
};

export default ImportRetryModal;
