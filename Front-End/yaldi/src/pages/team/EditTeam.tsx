import React, { useCallback, useEffect, useState } from "react";
import ModalSmall from "../../components/common/ModalSmall";
import FilledButton from "../../components/common/FilledButton";
import InputBox from "../../components/common/InputBox";
import { type Team } from "../../types/teams";
import { apiController } from "../../apis/apiController";

interface EditTeamProps {
  isOpen: boolean;
  team: Team;
  onClose: () => void;
  onSuccess: (updatedTeam: Team) => void; // ← 성공 시 상위에 알림
  onDeleteClick: () => void;
}

const EditTeam: React.FC<EditTeamProps> = ({
  isOpen,
  team,
  onClose,
  onSuccess,
  onDeleteClick,
}) => {

  const [nameInput, setNameInput] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);


  // 모달이 열릴 때마다 현재 이름으로 초기화
  useEffect(() => {
    if (isOpen) {
      setNameInput(team.name);
      setErrorMessage(null);
    }
  }, [isOpen, team]);


  const handleSubmit = useCallback(async () => {
    setErrorMessage(null);
    const name = nameInput.trim();

    if (name.length === 0) {
      setErrorMessage("팀 이름을 입력해주세요.");
      return;
    }

    if (name.length > 25) {
      setErrorMessage("팀 이름은 25자 이내여야 합니다.");
      return;
    }

    if (name === team.name) {
      setErrorMessage("기존 이름과 동일합니다.");
      return;
    }

    setIsSubmitting(true);
    try {
      console.log(`[팀 이름 수정] 새 이름: ${name}`);

      const response = await apiController({
        url: `/api/v1/teams/${team.teamKey}`,
        method: 'patch',
        data: {
          name,
        }
      })

      onSuccess(response.data.result); // 상위에 알려서 상태 업데이트
      onClose(); // 모달 닫기

    } catch (error) {
      const err = error as Error;

      console.error('[팀 이름 수정 실패]', err);

    } finally {
      setIsSubmitting(false);
    }
  }, [nameInput, team, onSuccess, onClose]);

  const handleEnterPress = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      e.currentTarget.blur();
      handleSubmit();
    }
  }, [handleSubmit]);



  return (
    <ModalSmall isOpen={isOpen} onClose={onClose} title="팀 정보 수정">
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>
        {/* 인풋 영역 */}
        <div className='flex w-full text-my-black items-start justify-between gap-4'>
          <div className="flex-shrink-0 pt-2 text-lg font-medium w-24">
            팀 이름
          </div>
          <div className="flex flex-col flex-grow justify-start gap-2">
            <InputBox
              placeholder="팀 이름을 입력하세요"
              className="w-full"
              value={nameInput}
              onChange={(e) => { setNameInput(e.target.value); setErrorMessage("") }}
              onKeyDown={handleEnterPress}
              disabled={isSubmitting}
              maxLength={25}
              required
            />
            <div className="text-xs text-gray-500">
              {nameInput.length}/25
            </div>
            {errorMessage && (
              <div className="text-sm text-red-500 font-medium ">
                * {errorMessage}
              </div>
            )}
          </div>
        </div>


        <div className='flex w-full justify-end items-center gap-4'>
          <button
            onClick={onDeleteClick}
            className="px-4 py-2 text-sm font-medium text-gray-500  hover:text-my-border transition-colors"
          >
            팀 삭제
          </button>
          <FilledButton
            label="저장하기"
            onClick={handleSubmit}
            disabled={isSubmitting}
          />
        </div>
      </div>
    </ModalSmall>
  );
}


export default EditTeam;