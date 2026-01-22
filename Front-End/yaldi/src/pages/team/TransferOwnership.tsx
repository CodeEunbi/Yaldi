import React from "react";
import ModalSmall from "../../components/common/ModalSmall";
import FilledButton from "../../components/common/FilledButton";

interface transferOwnershipProps {
  isOpen: boolean;
  userKey: number | null;
  onClose: () => void;
  onConfirm: (userKey: number | null) => void;
}


const TransferOwnership: React.FC<transferOwnershipProps> = ({
  isOpen,
  userKey,
  onClose,
  onConfirm
}) => {

  const handleConfirm = () => {
    onConfirm(userKey);
    onClose();
  }

  if (!userKey) return null;

  return (
    <ModalSmall isOpen={isOpen} onClose={onClose} title="팀장 권한 위임">
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>
        <div className='flex w-full text-my-black items-start justify-between gap-4'>
          이 멤버에게 팀장 권한을 위임하시겠습니까?
        </div>

        <div className='flex w-full justify-end items-center gap-4'>
          <FilledButton
            label="확인"
            onClick={handleConfirm}
          />
        </div>
      </div>

    </ModalSmall>
  )
}

export default TransferOwnership;