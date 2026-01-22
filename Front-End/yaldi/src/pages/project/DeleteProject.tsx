import React, { useCallback, useState } from 'react';
import ModalSmall from '../../components/common/ModalSmall';
import WarningButton from '../../components/common/WarningButton';


interface modalProps {
  projectKey: number | null;
  isOpen: boolean;
  onConfirm: (projectKey: number) => void;
  onClose: () => void;

}

const DeleteProject: React.FC<modalProps> = ({
  projectKey,
  isOpen,
  onClose,
  onConfirm,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleConfirm = useCallback(async () => {
    if (!projectKey) {
      console.error('projectKey가 없습니다.');
      return;
    }

    setIsSubmitting(true);
    try {
      
      // 🎯 부모에게서 받은 함수 실행
      await onConfirm(projectKey);
      
      // 성공 시 onConfirm 안에서 모달을 닫아주므로 여기서는 안 닫음
      
    } catch (error) {
      console.error('[모달] 프로젝트 삭제 실패:', error);
      // 에러 처리는 부모에서 했지만, 여기서 추가 처리 가능
    } finally {
      setIsSubmitting(false);
    }
  }, [projectKey, onConfirm]);

  return (
    <ModalSmall title="프로젝트 삭제 확인" isOpen={isOpen} onClose={onClose}>
      <div className='flex w-full flex-col gap-2.5 text-my-black'>
        <div>
          프로젝트의 모든 데이터가 삭제되며, 복구할 수 없습니다.
          <br />
          정말로 프로젝트를 삭제하시겠습니까?
        </div>
        <div className='flex w-full justify-end'>
          <WarningButton
            label='삭제하기'
            onClick={handleConfirm}
            disabled={isSubmitting || !projectKey}

          />
        </div>
      </div>
    </ModalSmall>
  );
}

export default DeleteProject;