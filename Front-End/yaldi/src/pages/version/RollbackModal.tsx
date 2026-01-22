import React, { useCallback } from "react";
import ModalSmall from "../../components/common/ModalSmall";
import FilledButton from "../../components/common/FilledButton";

interface RollbackModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

const RollbackModal: React.FC<RollbackModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
}) => {

  const [isSubmitting, setIsSubmitting] = React.useState<boolean>(false);


  // ESC 또는 닫기 버튼으로 모달 닫힐 때 상태 초기화
  const handleCloseModal = useCallback(() => {
    // setErrorMessage(null);
    onClose();
  }, [onClose]);

  const handleConfirm = () => {
    // try, catch로 에러 처리 가능
    setIsSubmitting(true);
    try {
      onConfirm();
    } catch (error) {
      console.error("오류 발생:", error);
    } finally {
      // 모달 닫기
      setIsSubmitting(false);
    }
    onClose();
  };


  return (
    <ModalSmall title="이 버전으로 되돌리기" isOpen={isOpen} onClose={handleCloseModal}>
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>
        <div className='flex w-full text-my-black items-center justify-between gap-4'>
          현재 워크스페이스의 상태를 이 버전으로 되돌리시겠습니까?
        </div>


        {/* 버튼 */}
        <div className='flex w-full justify-end items-center gap-4'>
          <FilledButton label="되돌리기" onClick={handleConfirm} disabled={isSubmitting} />
        </div>
      </div>

    </ModalSmall >
  );
};
export default RollbackModal;