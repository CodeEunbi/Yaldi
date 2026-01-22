import React, { useState, useRef, useEffect, useMemo } from 'react';
import ModalSmall from '../../components/common/ModalSmall';
import FilledButton from '../../components/common/FilledButton';
import InputBox from '../../components/common/InputBox';
import type { getTeamMemberListResponse } from '../../types/teams';
import { theme } from '../../styles/theme';
import { compressImage } from '../../utils/imageCompression';
import { useGetProjectInfo } from '../../hooks/useGetProjectInfo';
import { useGetProjectMemberList } from "../../hooks/useGetProjectMemberList";
import { useUpdateProject } from '../../hooks/useUpdateProject';
import type { addProjectMemberRequestItem, getProjectMemberListResponseItem } from '../../types/project';



interface modalProps {
  isOpen: boolean;
  userKey: number;
  teamKey: number;
  projectKey: number;
  teamMembers: getTeamMemberListResponse[];
  onClose: () => void;
}

const EditProject: React.FC<modalProps> = ({
  isOpen,
  userKey,
  // teamKey,
  projectKey,
  teamMembers,
  onClose,
}) => {

  const {
    isLoading: isLoadingProject,
    data: projectData,
    error: projectError,
  } = useGetProjectInfo(projectKey);

  const {
    isLoading: isLoadingMember,
    data: memberListData,
    error: memberError
  } = useGetProjectMemberList(projectKey);

  const { updateProject, isLoading: isUpdatingProject, error: updateError } = useUpdateProject();

  const [selectedMembers, setSelectedMembers] = useState<addProjectMemberRequestItem[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [name, setName] = useState<string>("");
  const [description, setDescription] = useState<string>("");
  const [imageUrl, setImageUrl] = useState<string>("");
  const [imagePreview, setImagePreview] = useState<string>("");
  const [isCompressing, setIsCompressing] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 프로젝트 데이터가 로드되면 초기값 설정
  useEffect(() => {
    if (projectData) {
      setName(projectData.name || "");
      setDescription(projectData.description || "");
      setImageUrl(projectData.imageUrl || "");
      setImagePreview(projectData.imageUrl || "");
    }
  }, [projectData]);

  // 멤버 리스트가 로드되면 초기값 설정
  useEffect(() => {
    if (memberListData) {
      setSelectedMembers(memberListData.map(member => ({
        memberKey: member.memberKey,
        role: member.role as "OWNER" | "EDITOR" | "ADMIN"
      })));
    }
  }, [memberListData]);

  // 변경사항이 있는지 체크
  const hasChanges = useMemo(() => {
    if (!projectData || !memberListData) return false;

    // 1. name 변경 체크
    const nameChanged = name !== (projectData.name || "");

    // 2. description 변경 체크
    const descriptionChanged = description !== (projectData.description || "");

    // 3. imageUrl 변경 체크
    const imageUrlChanged = imageUrl !== (projectData.imageUrl || "");

    // 4. 멤버 목록 변경 체크
    const originalMemberKeys = new Set<number>(memberListData.map((m: getProjectMemberListResponseItem) => m.memberKey));
    const selectedMemberKeys = new Set<number>(selectedMembers.map((m: addProjectMemberRequestItem) => m.memberKey));

    const membersChanged =
      originalMemberKeys.size !== selectedMemberKeys.size ||
      Array.from<number>(originalMemberKeys).some(key => !selectedMemberKeys.has(key));

    return nameChanged || descriptionChanged || imageUrlChanged || membersChanged;
  }, [projectData, memberListData, name, description, imageUrl, selectedMembers]);

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 이미지 파일인지 확인
    if (!file.type.startsWith('image/')) {
      alert('이미지 파일만 업로드 가능합니다.');
      return;
    }

    // 파일 크기 체크 (예: 10MB)
    if (file.size > 10 * 1024 * 1024) {
      alert('파일 크기는 10MB 이하여야 합니다.');
      return;
    }

    try {
      setIsCompressing(true);
      const compressedImage = await compressImage(file);
      setImageUrl(compressedImage);
      setImagePreview(compressedImage);
    } catch (error) {
      console.error('이미지 압축 실패:', error);
      alert('이미지 처리 중 오류가 발생했습니다.');
    } finally {
      setIsCompressing(false);
    }
  };

  const handleRemoveImage = () => {
    setImageUrl("");
    setImagePreview("");
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleImageClick = () => {
    fileInputRef.current?.click();
  };

  const handleClose = () => {
    // 상태 초기화
    setName("");
    setDescription("");
    setImageUrl("");
    setImagePreview("");
    setSelectedMembers([]);
    setSearchQuery("");
    // 모달 닫기
    onClose();
  };

  // 팀원 검색 필터링
  const filteredMembers = teamMembers.filter((member) => {
    // 현재 사용자는 제외
    if (member.userKey === userKey) return false;
    // 이미 선택된 멤버는 제외
    if (selectedMembers.some(selected => selected.memberKey === member.userKey)) return false;
    // 검색어가 없으면 모두 표시
    if (!searchQuery.trim()) return true;
    // 닉네임 또는 이메일로 검색
    const query = searchQuery.toLowerCase();
    const nickname = member.nickName?.toLowerCase() || '';
    // const email = member.email?.toLowerCase() || '';
    // return nickname.includes(query) || email.includes(query);
    return nickname.includes(query);
  });

  // 팀원 선택
  const handleSelectMember = (member: getTeamMemberListResponse) => {

    setSelectedMembers(prev => [...prev, { memberKey: member.userKey, role: member.isOwner ? "ADMIN" : "EDITOR" }]);
    setSearchQuery(''); // 선택 후 검색어 초기화
  };

  // 팀원 선택 취소
  const handleRemoveMember = (memberKey: number) => {
    setSelectedMembers(prev => prev.filter(member => member.memberKey !== memberKey));
  };

  const handleSubmit = async () => {
    // 필수 값 검증
    if (!name.trim()) {
      alert('프로젝트 이름을 입력해주세요.');
      return;
    }

    if (isCompressing) {
      alert('이미지 압축 중입니다. 잠시만 기다려주세요.');
      return;
    }

    if (!projectData || !memberListData) {
      alert('프로젝트 정보를 불러오는 중입니다. 잠시만 기다려주세요.');
      return;
    }

    try {
      setIsSubmitting(true);

      // 변경된 필드만 포함하는 객체 생성
      const updateParams: {
        projectKey: number;
        name?: string;
        description?: string;
        imageUrl?: string;
        originalMembers: addProjectMemberRequestItem[];
        newMembers: addProjectMemberRequestItem[];
      } = {
        projectKey,
        originalMembers: memberListData.map(member => ({
          memberKey: member.memberKey,
          role: member.role as "OWNER" | "EDITOR" | "ADMIN"
        })),
        newMembers: selectedMembers
      };

      // 변경된 프로젝트 정보만 추가
      if (name !== (projectData.name || "")) {
        updateParams.name = name;
      }
      if (description !== (projectData.description || "")) {
        updateParams.description = description;
      }
      if (imageUrl !== (projectData.imageUrl || "")) {
        updateParams.imageUrl = imageUrl;
      }

      await updateProject(updateParams);

      alert('프로젝트가 성공적으로 수정되었습니다.');
      handleClose();

    } catch (error) {
      console.error('프로젝트 정보 수정 실패:', error);
      alert(updateError || '프로젝트 정보 수정에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalSmall title="프로젝트 생성" isOpen={isOpen} onClose={handleClose}>
      {/* 자식 컴포넌트 */}
      {isLoadingMember || isLoadingProject ? (
        <div className="w-full pt-60 flex justify-center items-center">
          {/* 스피너 */}
          <div role="status">
            <svg aria-hidden="true" className="w-8 h-8 text-gray-200 animate-spin dark:text-gray-600 fill-blue-600" viewBox="0 0 100 101" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z" fill='#CFCFCF' />
              <path d="M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z" fill='#1E50AF' />
            </svg>
            <span className="sr-only">Loading...</span>
          </div>
        </div>
      ) : memberError ? (
        <div className="w-full pt-60 flex justify-center items-center">{memberError}</div>
      ) : projectError ? (
        <div className="w-full pt-60 flex justify-center items-center">{projectError}</div>
      ) : (
        <div className='flex w-full flex-col justify-start gap-2.5 text-my-black'>
          <div className='text-xs'><span className='text-warning'>*</span>은 필수 입력값입니다.</div>
          {/* 인풋 영역 */}
          <div className='flex flex-col w-full justify-normal gap-2.5'>
            <div className='w-full flex flex-col'>
              <label className='text-xl font-semibold pb-2' htmlFor="projectName">
                프로젝트 이름
                <span className='text-warning'>*</span>
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
              <div className="text-xs text-gray-500">
                {name.length}/25
              </div>
            </div>

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
              {description.length >= 1000 && <div className='text-sm'>* 1000자 이내로 작성해주세요.</div>}
            </div>

            {/* 멤버 선택 */}
            <div className='w-full flex flex-col'>
              <label className='text-xl font-semibold pb-2' htmlFor="searchMembers">
                멤버
              </label>

              {/* 팀원 검색 창 */}
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
                        {/* {member.email && (
                          <span className='text-xs text-gray-500'>{member.email}</span>
                        )} */}
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
                      key={member.memberKey}
                      className={`flex items-center gap-2 px-3 py-1.5 bg-${theme.myLightBlue}  text-myBlack rounded-full`}
                    >
                      <span className='text-sm font-medium'>{member.memberKey}</span>
                      <button
                        type='button'
                        onClick={() => handleRemoveMember(member.memberKey)}
                        className='hover:bg-white hover:bg-opacity-20 rounded-full w-5 h-5 flex items-center justify-center transition-all'
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>


            {/* 이미지 입력 */}
            <div className='w-full flex flex-col'>
              <label className='text-xl font-semibold pb-2' htmlFor="projectImage">
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
              label={isSubmitting || isUpdatingProject ? '수정 중...' : '수정하기'}
              onClick={handleSubmit}
              disabled={isSubmitting || isUpdatingProject || isCompressing || !name.trim() || !hasChanges}
            />
          </div>
        </div>
      )}
    </ModalSmall>
  );
}

export default EditProject;