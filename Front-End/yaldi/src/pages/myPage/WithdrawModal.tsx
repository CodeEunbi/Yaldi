import React from 'react';
import ModalSmall from '../../components/common/ModalSmall';
import WarningButton from '../../components/common/WarningButton';
import Swal from 'sweetalert2';
import { apiController } from '../../apis/apiController';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';


interface ModalProps extends React.ComponentPropsWithoutRef<'div'> {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
}


const WithdrawModal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  ...rest
}) => {

  const navigate = useNavigate();
  const logout = useAuthStore((state) => state.logout);
  const setProjectKey = useAuthStore((state) => state.setProjectKey);
  const setProjectName = useAuthStore((state) => state.setProjectName);

  const WITHDRAW_MESSAGE: string = "삭제된 계정은 복구할 수 없습니다. 정말로 탈퇴하시겠습니까?";

  const handleWithdraw = async () => {
    console.log("진짜 탈퇴 요청.");
    try {
      await apiController({
        url: `/api/v1/users/me`,
        method: 'delete'
      })

      Swal.fire({
        text: '지금까지 Yaldi를 이용해주셔서 감사합니다.',
        confirmButtonColor: '#1e50af',
      })
      setProjectKey(null);
      setProjectName('');
      logout();
      navigate("/", { replace: true });
    } catch (err) {
      console.log("탈퇴 실패", err);
      Swal.fire({
        text: "일시적인 오류로 요청을 처리하지 못했습니다.",
        icon: 'error',
        confirmButtonColor: '#1e50af',
      })
    }
    onConfirm();
  }

  return (
    <ModalSmall isOpen={isOpen} onClose={onClose} title="탈퇴" {...rest}>
      {/* children Prop으로 전달할 Content를 여기에 직접 렌더링합니다. */}
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>

        <div className='flex w-full text-my-black items-center justify-between gap-4'>
          {WITHDRAW_MESSAGE}
        </div>
        <div className='flex w-full justify-end items-center gap-4'>
          <WarningButton label="탈퇴하기" onClick={handleWithdraw} />
        </div>
      </div>
    </ModalSmall>
  )
};

export default WithdrawModal;