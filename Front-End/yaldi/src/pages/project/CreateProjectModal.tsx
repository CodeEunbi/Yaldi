import React, { useState, useRef } from 'react';
import ModalSmall from '../../components/common/ModalSmall';
import FilledButton from '../../components/common/FilledButton';
import InputBox from '../../components/common/InputBox';
import type { TeamMember } from '../../types/teams';
import { theme } from '../../styles/theme';
import { compressImageToMaxLength } from '../../utils/imageCompression';
import { useCreateProject } from '../../hooks/useCreateProject';
import { useAddProjectMember } from '../../hooks/useAddProjectMember';
import ToggleButton from '../../components/common/ToggleButton';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';

interface modalProps {
  isOpen: boolean;
  userKey: number;
  teamKey: number;
  teamMembers: TeamMember[];
  onClose: () => void;
}

const CreateProject: React.FC<modalProps> = ({
  isOpen,
  userKey,
  teamKey,
  teamMembers,
  onClose,
}) => {
  const navigate = useNavigate();

  const { createProject } = useCreateProject();
  const { addMembers } = useAddProjectMember();

  const [selectedMembers, setSelectedMembers] = useState<TeamMember[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [name, setName] = useState<string>("");
  const [description, setDescription] = useState<string>("");

  // 이미지 상태 - base64 문자열로 저장
  const [imageBase64, setImageBase64] = useState<string>("");
  const [imagePreview, setImagePreview] = useState<string>("");

  const [isCompressing, setIsCompressing] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [aiAssistant, setAiAssistant] = useState<boolean>(false);
  const [aiAssistantContent, SetAiAssistantContent] = useState<string>("");
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 이미지 변경 핸들러 - 3000자 이내로 압축
  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 이미지 파일인지 확인
    if (!file.type.startsWith('image/')) {
      Swal.fire({
        icon: 'error',
        text: '이미지 파일만 업로드 가능합니다.',
        confirmButtonColor: '#1e50af',
      });
      return;
    }

    // 파일 크기 체크 (10MB)
    if (file.size > 10 * 1024 * 1024) {
      Swal.fire({
        icon: 'error',
        text: '파일 크기는 10MB 이하여야 합니다.,',
        confirmButtonColor: '#1e50af',
      });
      return;
    }

    try {
      setIsCompressing(true);

      // 3000자 이내의 base64로 압축
      const compressedBase64 = await compressImageToMaxLength(file, 3000, 10);

      // base64를 state에 저장
      setImageBase64(compressedBase64);

      // 미리보기에도 동일한 base64 사용
      setImagePreview(compressedBase64);

    } catch (error) {
      console.error('이미지 압축 실패:', error);
      Swal.fire({
        icon: 'error',
        text: error instanceof Error ? error.message : '이미지 처리 중 오류가 발생했습니다.',
        confirmButtonColor: '#1e50af',
      });
    } finally {
      setIsCompressing(false);
    }
  };

  // 이미지 제거 핸들러
  const handleRemoveImage = () => {
    setImageBase64("");
    setImagePreview("");

    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleImageClick = () => {
    fileInputRef.current?.click();
  };

  // 모달 닫기 핸들러
  const handleClose = () => {
    // 상태 초기화
    setName("");
    setDescription("");
    setImageBase64("");
    setImagePreview("");
    setSelectedMembers([]);
    setSearchQuery("");
    setAiAssistant(false);
    SetAiAssistantContent("");

    // 모달 닫기
    onClose();
  };

  // 팀원 검색 필터링
  const filteredMembers = teamMembers.filter((member) => {
    if (member.userKey === userKey) return false;
    if (selectedMembers.some(selected => selected.userKey === member.userKey)) return false;
    if (!searchQuery.trim()) return true;

    const query = searchQuery.toLowerCase();
    const nickname = member.nickname?.toLowerCase() || '';
    const nickName = member.nickName?.toLowerCase() || '';
    const email = member.email?.toLowerCase() || '';
    return nickname.includes(query) || email.includes(query) || nickName.includes(query);
  });

  const handleSelectMember = (member: TeamMember) => {
    setSelectedMembers(prev => [...prev, member]);
    setSearchQuery('');
  };

  const handleRemoveMember = (memberKey: number) => {
    setSelectedMembers(prev => prev.filter(member => member.userKey !== memberKey));
  };

  // 프로젝트 생성 제출 핸들러
  const handleSubmit = async () => {
    // 필수 값 검증
    if (!name.trim()) {
      Swal.fire({
        icon: 'warning',
        text: '프로젝트 이름을 입력해주세요.',
        confirmButtonColor: '#1e50af',
      });
      return;
    }

    if (isCompressing) {
      Swal.fire({
        icon: 'info',
        text: '이미지 압축 중입니다. 잠시만 기다려주세요.',
        confirmButtonColor: '#1e50af',
      });
      return;
    }

    // Base64 길이 확인 (3000자 제한)
    if (imageBase64 && imageBase64.length > 3000) {
      Swal.fire({
        icon: 'error',
        text: `이미지 크기가 너무 큽니다. (${imageBase64.length}자 / 3000자)`,
        confirmButtonColor: '#1e50af',
      });
      return;
    }

    try {
      setIsSubmitting(true);

      // 프로젝트 생성 (imageBase64는 이미 3000자 이내로 압축됨)
      const newProject = await createProject(
        teamKey,
        name,
        description.trim() || undefined,
        imageBase64 || undefined
      );

      // 선택된 멤버가 있으면 초대
      if (selectedMembers.length > 0 && newProject.projectKey) {
        try {
          await addMembers(newProject.projectKey, {
            members: selectedMembers.map(member => ({
              memberKey: member.userKey,
              role: 'EDITOR'
            }))
          });
          console.log('✅ 멤버 초대 완료');
        } catch (memberError) {
          console.error('멤버 초대 실패:', memberError);
          Swal.fire({
            icon: 'warning',
            text: '프로젝트는 생성되었으나 일부 멤버 초대에 실패했습니다.',
            confirmButtonColor: '#1e50af',
          });
        }
      }

      // 성공 후 처리
      Swal.fire({
        icon: 'success',
        title: '프로젝트가 생성되었습니다!',
        showConfirmButton: false,
        timer: 2000
      });

      handleClose();
      navigate(`/project/${newProject.projectKey}/workspace`);

    } catch (error) {
      console.error('프로젝트 생성 실패:', error);
      Swal.fire({
        icon: 'error',
        text: '프로젝트 생성에 실패했습니다. 다시 시도해주세요.',
        confirmButtonColor: '#1e50af',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalSmall title="프로젝트 생성" isOpen={isOpen} onClose={handleClose}>
      <div className='flex w-full flex-col justify-start gap-2.5 text-my-black'>
        <div className='text-xs'><span className='text-warning'>*</span>은 필수 입력값입니다.</div>

        {/* 인풋 영역 */}
        <div className='flex flex-col w-full justify-normal gap-2.5'>
          {/* 프로젝트 이름 */}
          <div className='w-full flex flex-col'>
            <label className='text-xl font-semibold pb-2' htmlFor="projectName">
              프로젝트 이름<span className='text-warning'>*</span>
            </label>
            <InputBox
              id='projectName'
              size="w-full p-2"
              placeholder='프로젝트의 이름을 입력해주세요.'
              value={name}
              onChange={e => setName(e.target.value)}
              maxLength={25}
              required
            />
            <div className="text-xs text-gray-500">{name.length}/25</div>
          </div>

          {/* 설명 */}
          <div className='w-full flex flex-col'>
            <label className='text-xl font-semibold pb-2' htmlFor="projectDescription">
              설명 (선택)
            </label>
            <textarea
              id='projectDescription'
              placeholder='프로젝트에 대한 설명을 적어주세요.'
              value={description}
              onChange={e => setDescription(e.target.value)}
              maxLength={1000}
              className={`
                px-[20px] py-[10px]
                rounded-[10px]
                border-2 border-${theme.myBorder}
                focus:outline-none focus:border-${theme.myBlue}
                font-pretendard
              `}
            />
            {description.length >= 1000 && (
              <div className='text-sm text-warning'>* 1000자 이내로 작성해주세요.</div>
            )}
          </div>

          {/* 멤버 선택 */}
          <div className='w-full flex flex-col'>
            <label className='text-xl font-semibold pb-2' htmlFor="searchMembers">
              멤버
            </label>

            <div className='relative w-full'>
              <InputBox
                id='searchMembers'
                size="w-full p-2"
                placeholder='팀원 검색 (닉네임)'
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />

              {/* 검색 결과 드롭다운 */}
              {searchQuery.trim() && filteredMembers.length > 0 && (
                <div className='absolute z-10 w-full mt-1 max-h-48 overflow-y-auto bg-white border-2 border-my-border rounded-[10px] shadow-lg'>
                  {filteredMembers.map((member) => (
                    <button
                      key={member.userKey}
                      type='button'
                      onClick={() => handleSelectMember(member)}
                      className='w-full px-4 py-2 text-left hover:bg-gray-100 transition-colors flex flex-col'
                    >
                      <span className='font-medium'>{member.nickName}</span>
                      {member.email && (
                        <span className='text-xs text-gray-500'>{member.email}</span>
                      )}
                    </button>
                  ))}
                </div>
              )}

              {/* 검색 결과 없음 */}
              {searchQuery.trim() && filteredMembers.length === 0 && (
                <div className='absolute z-10 w-full mt-1 p-4 bg-white border-2 border-my-border rounded-[10px] shadow-lg text-center text-gray-500'>
                  검색 결과가 없습니다
                </div>
              )}
            </div>

            {/* 선택된 멤버 목록 */}
            {selectedMembers.length > 0 && (
              <div className='mt-3 flex flex-wrap gap-2'>
                {selectedMembers.map((member) => (
                  <div
                    key={member.userKey}
                    className={`flex items-center gap-2 px-3 py-1.5 bg-${theme.myLightBlue} text-myBlack rounded-full`}
                  >
                    <span className='text-sm font-medium'>{member.nickName}</span>
                    <button
                      type='button'
                      onClick={() => handleRemoveMember(member.userKey)}
                      className='hover:bg-white hover:bg-opacity-20 rounded-full w-5 h-5 flex items-center justify-center transition-all'
                    >
                      ✕
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* AI assistant */}
          <div className="w-full flex flex-col gap-2">
            <div className='text-xl font-semibold'>AI assistant</div>
            <ToggleButton
              onToggle={() => setAiAssistant(prev => !prev)}
              isOn={aiAssistant}
            />
            {aiAssistant && (
              <textarea
                id='AIAssistantInput'
                placeholder='어떤 프로젝트를 하는지 설명해 주세요. AI가 참고하여 유사한 프로젝트를 추천해 드릴게요.'
                value={aiAssistantContent}
                onChange={e => SetAiAssistantContent(e.target.value)}
                maxLength={1000}
                className={`
                  px-[20px] py-[10px]
                  rounded-[10px]
                  border-2 border-${theme.myBorder}
                  focus:outline-none focus:border-${theme.myBlue}
                  font-pretendard
                `}
              />
            )}
          </div>

          {/* 이미지 입력 */}
          <div className='w-full flex flex-col'>
            <label className='w-fit text-xl font-semibold pb-2' htmlFor="projectImage">
              이미지 (선택)
            </label>
            <div className='flex flex-col gap-2'>
              {/* 이미지 미리보기 */}
              {imagePreview && (
                <div className='relative w-full h-48 rounded-[10px] overflow-hidden border-2 border-my-border'>
                  <img
                    src={imagePreview}
                    alt="프로젝트 이미지 미리보기"
                    className='w-full h-full object-cover'
                  />
                  <button
                    type='button'
                    onClick={handleRemoveImage}
                    className='absolute top-2 right-2 bg-my-black bg-opacity-60 text-white rounded-full w-8 h-8 flex items-center justify-center hover:bg-opacity-80 transition-all'
                  >
                    ✕
                  </button>
                </div>
              )}

              {/* 이미지 업로드 버튼 */}
              <div className='flex items-center gap-2'>
                <button
                  type='button'
                  onClick={handleImageClick}
                  disabled={isCompressing}
                  className={`
                    px-[13px] py-[7px]
                    rounded-[10px]
                    border-2 border-${theme.myBlue}
                    text-${theme.myBlue}
                    text-sm
                    font-semibold
                    hover:border-${theme.myBlue}
                    focus:outline-none focus:border-${theme.myBlue}
                    font-pretendard
                    transition-all
                    ${isCompressing ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                  `}
                >
                  {isCompressing ? '압축 중...' : imagePreview ? '이미지 변경' : '이미지 선택'}
                </button>

                <input
                  ref={fileInputRef}
                  id='projectImage'
                  type='file'
                  accept='image/*'
                  onChange={handleImageChange}
                  className='hidden'
                />

                <span className='text-xs text-gray-500'>
                  JPG, PNG, GIF 등 (최대 10MB)
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* 제출 버튼 */}
        <div className='flex w-full justify-end'>
          <FilledButton
            label={isSubmitting ? '생성 중...' : '생성하기'}
            onClick={handleSubmit}
            disabled={isSubmitting || isCompressing || !name.trim()}
          />
        </div>
      </div>
    </ModalSmall>
  );
}

export default CreateProject;