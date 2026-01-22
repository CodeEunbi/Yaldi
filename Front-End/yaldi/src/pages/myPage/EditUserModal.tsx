import React, { useCallback, useState } from 'react';
import ModalSmall from '../../components/common/ModalSmall';
import InputBox from '../../components/common/InputBox';
import FilledButton from '../../components/common/FilledButton';
import { theme } from '../../styles/theme';
import WithdrawModal from "./WithdrawModal";
import { apiController } from '../../apis/apiController';
import Swal from 'sweetalert2';

interface ModalProps extends React.ComponentPropsWithoutRef<'div'> {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newName: string) => void;
  nickname: string;
}


const EditUserModal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  nickname,
  ...rest
}) => {

  // 탈퇴 모달 여는지
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
  // 닉네임 수정 입력값
  const [newNameInput, setNewNameInput] = useState(nickname);
  // 유효성 검사 메시지를 상태로 관리
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = useCallback(async () => {
    // 1. 유효성 초기화
    setErrorMessage(null);

    const newName = newNameInput.trim();

    // 1차 검사: 기존 닉네임과 동일한지 확인하는 로직 추가
    if (newName === nickname) {
      setErrorMessage("기존 닉네임과 동일합니다. 다른 닉네임을 입력해주세요.");
      return;
    }

    // 2. 유효성 검사 로직
    if (newName.length === 0) {
      setErrorMessage("닉네임을 입력해주세요.");
      return;
    }

    // 3. 제출 로직 실행
    console.log(`[수정 요청] 새 닉네임: ${newName}`);

    try {
      await apiController({
        url: '/api/v1/users/me/nickname',
        method: 'patch',
        data: {
          nickname: newName
        }
      })
      onSuccess(newName);
      onClose();
    } catch (err) {
      console.log('이름 수정 실패:', err);
      Swal.fire({
        text: '일시적인 에러로 요청에 실패했습니다. 다시 시도해 주세요.',
        icon: "error",
        confirmButtonColor: '#1e50af',
      })
    }

    // 4. 요청 성공 가정 후 모달 닫기 및 상태 초기화
  }, [newNameInput, onClose]);

  // ESC 또는 닫기 버튼으로 모달 닫힐 때 상태 초기화
  const handleCloseModal = useCallback(() => {
    setErrorMessage(null);
    onClose();
  }, [onClose]);


  const handleEnterPress = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault(); // 폼 제출 방지
      e.currentTarget.blur(); // 포커스 해제
      handleSubmit();
    }
  }, [handleSubmit]);

  return (
    <>
      <ModalSmall isOpen={isOpen} onClose={handleCloseModal} title="내 정보 수정" {...rest}>
        {/* children Prop으로 전달할 Content를 여기에 직접 렌더링합니다. */}

        <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>

          <div className='flex w-full text-my-black items-start justify-between gap-4'>
            {/* label 역할을 하는 div와 input 간의 간격 및 정렬 조정 */}
            <div className="flex-shrink-0 text-lg font-medium w-24">
              닉네임
            </div>
            {/* InputBox가 부모 flex 컨테이너의 남은 공간을 차지하도록 flex-grow 사용 */}
            <div className="flex flex-col flex-grow justify-start gap-2">
              {/* InputBox가 부모 flex-grow div의 너비를 채우도록 w-full 적용 */}
              <InputBox
                placeholder="사용할 닉네임을 입력하세요"
                className="w-full"
                value={newNameInput}
                onChange={(e) => setNewNameInput(e.target.value)} onKeyDown={handleEnterPress}
                required
              />
              {/* 💡 안내/오류 문구 렌더링 */}
              {errorMessage && (
                <div className="text-sm text-red-500 font-medium pt-1">
                  * {errorMessage}
                </div>
              )}
            </div>
          </div>
          <div className='flex w-full justify-end items-center gap-4'>
            <button className={`text-${theme.myBorder} hover:text-gray-500`} onClick={() => setIsWithdrawModalOpen(true)}>탈퇴하기</button>
            <FilledButton label="수정하기" onClick={handleSubmit} />
          </div>
        </div>
      </ModalSmall>
      <WithdrawModal isOpen={isWithdrawModalOpen} onClose={() => setIsWithdrawModalOpen(false)} onConfirm={() => { setIsWithdrawModalOpen(false); onClose(); }} />
    </>
  )
};

export default EditUserModal;