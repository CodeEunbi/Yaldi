import React, { useCallback, useEffect } from "react";
import ModalSmall from "../../components/common/ModalSmall";
import FilledButton from "../../components/common/FilledButton";
import InputBox from "../../components/common/InputBox";
// import { theme } from "../../styles/theme";

interface EditVersionModalProps {
  isOpen: boolean;
  name: string;
  description: string;
  onClose: () => void;
  onSubmit: (name: string, description: string) => void;
  // onClickDelete: () => void;
}

const EditVersionModal: React.FC<EditVersionModalProps> = ({
  isOpen,
  name,
  description,
  onClose,
  onSubmit,
  // onClickDelete,
}) => {

  useEffect(() => {
    setNewNameInput(name);
    setNewDescriptionInput(description);
  }, [name, description]);


  const [newNameInput, setNewNameInput] = React.useState<string>("");
  const [newDescriptionInput, setNewDescriptionInput] = React.useState<string>("");
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState<boolean>(false);


  // ESC 또는 닫기 버튼으로 모달 닫힐 때 상태 초기화
  const handleCloseModal = useCallback(() => {
    setErrorMessage(null);
    onClose();
  }, [onClose]);

  const handleSubmit = () => {
    // try, catch로 에러 처리 가능
    setIsSubmitting(true);
    try {
      // 유효성 검사 등 로직 추가 가능
      onSubmit(newNameInput, newDescriptionInput);
    } catch (error) {
      console.error("버전 정보 수정 중 오류 발생:", error);
      setErrorMessage("버전 정보 수정에 실패했습니다. 다시 시도해주세요.");
    } finally {
      // 모달 닫기
      setIsSubmitting(false);
    }
    onClose();
  };



  const handleEnterPress = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault(); // 폼 제출 방지
      e.currentTarget.blur(); // 포커스 해제
    }
  }, []);


  return (
    <ModalSmall title="버전 정보 수정" isOpen={isOpen} onClose={handleCloseModal}>
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>

        {/* 입력 영역 */}
        <div className='flex flex-col w-full text-my-black items-start justify-between gap-4'>

          <div className="w-full flex flex-col flex-grow justify-start gap-2">
            {/* InputBox가 부모 flex-grow div의 너비를 채우도록 w-full 적용 */}
            <label className="text-xl font-semibold pb-2" htmlFor="dataModelNameInput">
              버전 이름
            </label>
            <InputBox
              id="dataModelNameInput"
              placeholder="버전의 이름을 입력하세요"
              className="w-full"
              value={newNameInput}
              onChange={(e) => setNewNameInput(e.target.value)} onKeyDown={handleEnterPress}
              required
              max={255}
            />
            {newNameInput.length >= 255 && <div className='text-sm'>* 255자 이내로 작성해주세요.</div>}

            {/* 💡 안내/오류 문구 렌더링 */}
            {errorMessage && (
              <div className="text-sm text-red-500 font-medium pt-1">
                * {errorMessage}
              </div>
            )}
          </div>
          <div className='flex flex-col w-full justify-start gap-2'>
            <label className='text-xl font-semibold pb-2' htmlFor="DataModelDescription">
              설명
            </label>

            <textarea
              id='DataModelDescription'
              placeholder='버전에 대한 설명을 적어주세요.'
              value={newDescriptionInput}
              onChange={e => setNewDescriptionInput(e.target.value)}
              maxLength={1000}
              className={`
                w-full
                      px-[20px] py-[10px]
                      rounded-[10px]
                      border-2 border-my-border
                      focus:outline-none focus:border-blue
                      font-pretendard
                    `}
            // required
            />
            {newDescriptionInput.length >= 1000 && <div className='text-sm'>* 1000자 이내로 작성해주세요.</div>}
          </div>

        </div>

        {/* 버튼 */}
        <div className='flex w-full justify-end items-center gap-4'>
          {/* <button className={`text-${theme.myBorder} hover:text-gray-500`} onClick={onClickDelete}>삭제하기</button> */}
          <FilledButton label="수정하기" onClick={handleSubmit} disabled={isSubmitting} />
        </div>
      </div>

    </ModalSmall >
  );
};
export default EditVersionModal;