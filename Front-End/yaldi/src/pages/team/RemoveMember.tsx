import React from "react";
import ModalSmall from "../../components/common/ModalSmall";
import WarningButton from "../../components/common/WarningButton";

interface RemoveMemberProps {
  isOpen: boolean;
  userKey: number | null;
  onClose: () => void;
  onConfirm: (userKey: number | null) => void;
}


const RemoveMember: React.FC<RemoveMemberProps> = ({
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
    <ModalSmall isOpen={isOpen} onClose={onClose} title="팀원 내보내기">
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>
        <div className='flex w-full text-my-black items-start justify-between gap-4'>
          이 멤버를 팀에서 내보내시겠습니까?
        </div>

        <div className='flex w-full justify-end items-center gap-4'>
          <WarningButton
            label="내보내기"
            onClick={handleConfirm}
          />
        </div>
      </div>

    </ModalSmall>
  )
}

export default RemoveMember;