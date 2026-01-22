import React, { useCallback, useState } from 'react';
import ModalSmall from '../../components/common/ModalSmall';
import InputBox from '../../components/common/InputBox';
import FilledButton from '../../components/common/FilledButton';

interface ModalProps extends React.ComponentPropsWithoutRef<'div'> {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (newTeamName: string) => void;
}


const EditUserModal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  onCreate,
  ...rest
}) => {


  const [teamNameInput, setTeamNameInput] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);


  const handleSubmit = useCallback(() => {
    // 1. 유효성 초기화
    setErrorMessage(null);

    const teamName = teamNameInput.trim();

    // 2. 유효성 검사 로직
    if (teamName.length === 0) {
      setErrorMessage("팀의 이름을 입력해주세요.");
      return;
    }

    // 3. 제출 로직 실행
    console.log(`[팀 생성 요청] 팀 이름: ${teamName}`);

    // 4. 요청 성공 가정 후 모달 닫기 및 상태 초기화
    setErrorMessage("");
    setTeamNameInput("");
    onCreate(teamName);
    onClose();
  }, [teamNameInput, onClose]);

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
      <ModalSmall isOpen={isOpen} onClose={handleCloseModal} title="팀 생성" {...rest}>
        {/* children Prop으로 전달할 Content를 여기에 직접 렌더링합니다. */}

        <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>

          <div className='flex w-full text-my-black items-start justify-between gap-4'>
            {/* label 역할을 하는 div와 input 간의 간격 및 정렬 조정 */}
            <div className="flex-shrink-0 text-lg font-medium w-24">
              팀 이름
            </div>
            {/* InputBox가 부모 flex 컨테이너의 남은 공간을 차지하도록 flex-grow 사용 */}
            <div className="flex flex-col flex-grow justify-start gap-2">
              {/* InputBox가 부모 flex-grow div의 너비를 채우도록 w-full 적용 */}
              <InputBox
                placeholder="팀 이름을 입력하세요"
                className="w-full"
                value={teamNameInput}
                onChange={(e) => setTeamNameInput(e.target.value)} onKeyDown={handleEnterPress}
                required
              />
              {/* 현재 글자수 */}
              <div className="text-xs text-gray-500">
                {teamNameInput.length}/25
              </div>


              {/* 💡 안내/오류 문구 렌더링 */}
              {errorMessage && (
                <div className="text-sm text-red-500 font-medium pt-1">
                  * {errorMessage}
                </div>
              )}
            </div>
          </div>
          <div className='flex w-full justify-end items-center gap-4'>
            <FilledButton label="생성하기" onClick={handleSubmit} />
          </div>
        </div>
      </ModalSmall>
    </>
  )
};

export default EditUserModal;