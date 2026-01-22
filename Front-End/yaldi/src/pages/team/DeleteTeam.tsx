import React, { useCallback, useState } from 'react';
import ModalSmall from '../../components/common/ModalSmall';
import WarningButton from '../../components/common/WarningButton';


interface modalProps {
  teamKey: number | null;
  isOpen: boolean;
  onConfirm: () => void;
  onClose: () => void;

}

const DeleteTeam: React.FC<modalProps> = ({
  teamKey,
  isOpen,
  onClose,
  onConfirm,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleConfirm = useCallback(async () => {
    if (!teamKey) {
      console.error('teamKey가 없습니다.');
      return;
    }

    setIsSubmitting(true);
    try {
      console.log('[모달] 팀 삭제 확인:', teamKey);
      
      // 🎯 부모에게서 받은 함수 실행
      await onConfirm();
                
    } catch (error) {
      console.error('[모달] 팀 삭제 실패:', error);
      // 에러 처리는 부모에서 했지만, 여기서 추가 처리 가능
    } finally {
      setIsSubmitting(false);
    }
  }, [teamKey, onConfirm]);

  return (
    <ModalSmall title="팀 삭제 확인" isOpen={isOpen} onClose={onClose}>
      <div className='flex w-full justify-center flex-col gap-2.5 text-my-black'>
        <div>
          팀을 삭제하면 복구할 수 없으며
          <br />
          팀 프로젝트 중 공개된 프로젝트는 팀이 삭제되어도 다른 사람들에게 노출됩니다.
          <br  />
          정말로 팀을 삭제하시겠습니까?
        </div>
        <div className='flex w-full justify-end'>
          <WarningButton
            label='삭제하기'
            onClick={handleConfirm}
            disabled={isSubmitting || !teamKey}

          />
        </div>
      </div>
    </ModalSmall>
  );
}

export default DeleteTeam;