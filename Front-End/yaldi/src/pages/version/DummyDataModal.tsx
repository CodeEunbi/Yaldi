import React, { useCallback } from "react";
import ModalSmall from "../../components/common/ModalSmall";
import FilledButton from "../../components/common/FilledButton";
import InputBox from "../../components/common/InputBox";

interface DummyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (count: number) => Promise<void>;
}

const EditVersionModal: React.FC<DummyModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
}) => {


  const [countInput, setCountInput] = React.useState<number | null>(null);
  const [errorMessage, setErrorMessage] = React.useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState<boolean>(false);


  // ESC 또는 닫기 버튼으로 모달 닫힐 때 상태 초기화
  const handleCloseModal = useCallback(async () => {
    setErrorMessage(null);
    setCountInput(null);
    onClose();
  }, [onClose]);


  // 💡 1. handleSubmit을 비동기 함수로 변경
  const handleSubmit = useCallback(async () => {
    // 💡 countInput이 number 타입이 아닐 경우를 방어
    if (!countInput || countInput <= 0) {
      setErrorMessage("개수를 1개 이상 입력해주세요.");
      return;
    };

    setIsSubmitting(true);
    setErrorMessage(null); // 에러 메시지 초기화

    try {
      // 💡 onSubmit 함수를 await으로 기다립니다. (API 요청 가정)
      await onSubmit(countInput);

      // 성공 시: 상태 초기화 및 모달 닫기
      setCountInput(null);
      onClose();

    } catch (error) {
      console.error("더미 데이터 생성 요청 중 오류 발생:", error);
      setErrorMessage("더미 데이터 생성에 실패했습니다. 다시 시도해주세요.");
    } finally {
      // 💡 성공/실패와 관계없이 로딩 상태 해제
      setIsSubmitting(false);
    }
    // 🚨 모달 닫기 (onClose)는 성공 로직 내부로 이동했습니다.
  }, [countInput, onSubmit, onClose]);


  const handleEnterPress = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault(); // 폼 제출 방지
      e.currentTarget.blur(); // 포커스 해제

      // 💡 2. Enter 키가 눌렸을 때 제출 로직 실행
      handleSubmit();
    }
  }, [handleSubmit]); // handleSubmit 함수를 의존성 배열에 포함

  
  return (
    <ModalSmall title="더미 데이터 생성" isOpen={isOpen} onClose={handleCloseModal}>
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>

        {/* 입력 영역 */}
        <div className='flex flex-col w-full text-my-black items-start justify-between gap-4'>

          <div className="w-full flex flex-col flex-grow justify-start gap-2">
            {/* InputBox가 부모 flex-grow div의 너비를 채우도록 w-full 적용 */}
            <label className="text-xl font-semibold pb-2" htmlFor="dummyCountInput">
              버전 이름
            </label>
            <InputBox
              id="dummyCountInput"
              placeholder="생성할 더미 데이터의 개수를 입력해주세요."
              className="w-full"
              value={countInput ?? ""}
              required
              max={255}
              min={1}
              type="number"
              onChange={(e) => setCountInput(Number(e.target.value))} onKeyDown={handleEnterPress}
            />

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
          <FilledButton label="생성하기" onClick={handleSubmit} disabled={isSubmitting} />
        </div>
      </div>

    </ModalSmall >
  );
};
export default EditVersionModal;