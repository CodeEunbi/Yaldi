import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import SaveIcon from '../../../assets/icons/save_icon.svg?react';
import ModalSmall from '../../../components/common/ModalSmall';
import LinedButton from '../../../components/common/LinedButton';
import FilledButton from '../../../components/common/FilledButton';
import ToggleButton from '../../../components/common/ToggleButton';
import { apiController } from '../../../apis/apiController';
import Swal from 'sweetalert2';
import { useWorkspace } from '../WorkSpace';

interface VersionSaveProps {
  onModalChange?: (open: boolean) => void;
}

const VersionSave: React.FC<VersionSaveProps> = ({
  onModalChange,
}) => {
  const navigate = useNavigate();
  const projectKey = Number(useParams().projectKey);
  const { tables, relations } = useWorkspace();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [versionName, setVersionName] = useState('');
  const [versionDescription, setVersionDescription] = useState('');
  const [isPublic, setIsPublic] = useState(false);
  const [hasTriedSubmit, setHasTriedSubmit] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const isNameValid = versionName.trim().length > 0;

  useEffect(() => {
    if (!isModalOpen) {
      setVersionName('');
      setVersionDescription('');
      setIsPublic(false);
      setHasTriedSubmit(false);
    }
  }, [isModalOpen]);

  const handleConfirm = async () => {
    setHasTriedSubmit(true);
    if (!isNameValid) return;

    const trimmedName = versionName.trim();
    const trimmedDescription = versionDescription.trim();

    try {
      setIsLoading(true);

      const response = await apiController({
        url: `/api/v1/projects/${encodeURIComponent(projectKey)}/versions`,
        method: 'post',
        data: {
          name: trimmedName,
          description: trimmedDescription.length > 0 ? trimmedDescription : undefined,
          isPublic,
          schemaData: {
            tables,
            relations,
          },
        },
      });

      console.log('버전 저장 성공:', response);

      await Swal.fire({
        icon: 'success',
        title: '버전 저장 완료',
        text: `버전 "${trimmedName}"이 성공적으로 저장되었습니다.`,
        confirmButtonText: '확인',
      });

      setIsModalOpen(false);
      onModalChange?.(false);

      // 버전 목록 페이지로 이동
      navigate(`/project/${projectKey}/version`);
    } catch (err) {
      console.error('버전 저장 실패:', err);

      await Swal.fire({
        icon: 'error',
        title: '버전 저장 실패',
        text: '버전 저장 중 오류가 발생했습니다. 다시 시도해주세요.',
        confirmButtonText: '확인',
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = () => {
    setIsModalOpen(false);
    onModalChange?.(false);
  };

  return (
    <>
      {/* 저장 버튼 */}
      <button
        onClick={() => {
          setIsModalOpen(true);
          onModalChange?.(true);
        }}
        className="w-8 h-8 flex items-center justify-center mb-2 hover:bg-ai-chat rounded transition-colors"
        aria-label="저장"
      >
        <SaveIcon className="w-5 h-5 text-my-black" />
      </button>

      {/* 저장 모달 */}
      <ModalSmall isOpen={isModalOpen} onClose={handleCancel} title="버전 저장">
        <div className="flex flex-col gap-6 w-full py-2">
          <div className="flex flex-col gap-4">
            {/* 버전 이름 */}
            <label className="flex flex-col gap-2 text-my-black text-sm">
              <span className="text-base">
                버전 이름 <span className="text-my-red">*</span>
              </span>
              <input
                type="text"
                value={versionName}
                onChange={(e) => setVersionName(e.target.value)}
                placeholder="버전 이름을 입력하세요."
                className={`w-full rounded-[10px] border px-4 py-3 text-base ${
                  hasTriedSubmit && !isNameValid
                    ? 'border-my-red focus:border-my-red'
                    : 'border-my-border focus:border-my-blue'
                }`}
              />
              {hasTriedSubmit && !isNameValid && (
                <span className="text-xs text-my-red">
                  버전 이름은 필수 입력 항목입니다.
                </span>
              )}
            </label>

            {/* 설명 */}
            <label className="flex flex-col gap-2 text-my-black text-sm">
              <span className="text-base">
                버전 설명 <span className="text-my-black/40">(선택)</span>
              </span>
              <textarea
                rows={3}
                value={versionDescription}
                onChange={(e) => setVersionDescription(e.target.value)}
                placeholder="버전에 대한 설명을 입력하세요."
                className="w-full resize-none rounded-[10px] border border-my-border px-4 py-3 text-base focus:border-my-blue"
              />
            </label>

            {/* 공개 여부 */}
            <div className="flex items-center justify-between rounded-[10px] border border-my-border px-4 py-3">
              <span className="text-base text-my-black">공개 여부</span>
              <div className="flex items-center gap-3">
                <span className="text-sm text-my-black">
                  {isPublic ? 'Public' : 'Private'}
                </span>
                <ToggleButton
                  isOn={isPublic}
                  onToggle={() => setIsPublic((prev) => !prev)}
                  className="h-[30px]"
                />
              </div>
            </div>
          </div>

          {/* 버튼 */}
          <div className="flex gap-3 justify-center">
            <LinedButton
              label="취소"
              onClick={handleCancel}
              size="px-6 py-2"
              disabled={isLoading}
            />
            <FilledButton
              label={isLoading ? '저장 중...' : '확인'}
              onClick={handleConfirm}
              disabled={!isNameValid || isLoading}
              size="px-6 py-2"
            />
          </div>
        </div>
      </ModalSmall>
    </>
  );
};

export default VersionSave;
