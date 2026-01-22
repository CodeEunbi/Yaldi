import React, { useCallback, useEffect } from "react";
import ModalSmall from "../../components/common/ModalSmall";
import FilledButton from "../../components/common/FilledButton";
import InputBox from "../../components/common/InputBox";
import { theme } from "../../styles/theme";

interface EditDataModelModalProps {
  isOpen: boolean;
  name: string;
  onClose: () => void;
  onSubmit: (name: string) => void;
  onClickDelete: () => void;
}

const EditDataModelModal: React.FC<EditDataModelModalProps> = ({
  isOpen,
  name,
  onClose,
  onSubmit,
  onClickDelete,
}) => {

  useEffect(() => {
    setNewNameInput(name);
  }, [name]);


  const [newNameInput, setNewNameInput] = React.useState<string>("");
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
      onSubmit(newNameInput);
    } catch (error) {
      console.error("데이터 모델 수정 중 오류 발생:", error);
      setErrorMessage("데이터 모델 수정에 실패했습니다. 다시 시도해주세요.");
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
    <ModalSmall title="데이터 모델 정보 수정" isOpen={isOpen} onClose={handleCloseModal}>
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>

        {/* 입력 영역 */}
        <div className='flex flex-col w-full text-my-black items-start justify-between gap-4'>

          <div className="w-full flex flex-col flex-grow justify-start gap-2">
            {/* InputBox가 부모 flex-grow div의 너비를 채우도록 w-full 적용 */}
            <label className="text-xl font-semibold pb-2" htmlFor="dataModelNameInput">
              이름
            </label>
            <InputBox
              id="dataModelNameInput"
              placeholder="데이터 모델의 이름을 입력하세요"
              className="w-full"
              value={newNameInput}
              onChange={(e) => setNewNameInput(e.target.value)} onKeyDown={handleEnterPress}
              required
              max={500}
            />
            {newNameInput.length >= 500 && <div className='text-sm'>* 500자 이내로 작성해주세요.</div>}

            {/* 💡 안내/오류 문구 렌더링 */}
            {errorMessage && (
              <div className="text-sm text-red-500 font-medium pt-1">
                * {errorMessage}
              </div>
            )}
          </div>
        </div>

        {/* 버튼 */}
        <div className='flex w-full justify-end items-center gap-4'>
          <button className={`text-${theme.myBorder} hover:text-gray-500`} onClick={onClickDelete}>삭제하기</button>
          <FilledButton label="수정하기" onClick={handleSubmit} disabled={isSubmitting} />
        </div>
      </div>

    </ModalSmall >
  );
};
export default EditDataModelModal;