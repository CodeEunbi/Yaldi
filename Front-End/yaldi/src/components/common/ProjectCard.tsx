import React from 'react';
import DeleteIcon from '../../assets/icons/delete_icon.svg?react';
import LeaveIcon from "../../assets/icons/logout_icon.svg?react";
import { useDeleteProject } from '../../hooks/useDeleteProject';
import DeleteProject from '../../pages/project/DeleteProject';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';
import { apiController } from '../../apis/apiController';


// 모든 표준 HTML div 속성을 상속받도록 정의
interface ProjectCardProps extends React.ComponentPropsWithoutRef<'div'> {
  projectKey: number;
  name: string;
  // ownedBy: number;
  imageUrl?: string;
  updatedAt: string;
  isOwner: boolean;
  onProjectDeleted?: (projectKey: number) => void;
}


const ProjectCard: React.FC<ProjectCardProps> = ({
  projectKey,
  name,
  imageUrl,
  updatedAt,
  isOwner,
  onProjectDeleted,
  className,   // 기존 className을 받아 루트 요소에 적용
  ...rest
}) => {

  const navigate = useNavigate();

  const { isDeleteModalOpen, projectKeyToDelete, openDeleteModal, closeDeleteModal, confirmDelete } = useDeleteProject(onProjectDeleted);

  // 💡 조건부 클래스 정의: OWNER가 아닐 때만 "hidden" 클래스 반환
  const deleteButtonHiddenClass = isOwner ? "" : "hidden";

  // 카드 클릭 핸들러 - workspace로 이동
  const handleCardClick = () => {
    navigate(`/project/${projectKey}/workspace`);
  };

  // 💡 onCardClick 이벤트와의 충돌 방지 핸들러
  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation(); // 카드 전체 클릭 이벤트가 버튼 클릭 시 실행되는 것을 방지
    openDeleteModal(projectKey);
  };

  const handleLeaveProject = async (e: React.MouseEvent) => {
    e.stopPropagation();

    const result = await Swal.fire({
      title: '프로젝트 나가기',
      text: '프로젝트를 나가시겠습니까?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '나가기',
      cancelButtonText: '취소',
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#1e50af'
    });

    // 사용자가 취소를 누른 경우 함수 종료
    if (!result.isConfirmed) {
      return;
    }

    try {
      await apiController({
        url: `/api/v1/projects/${projectKey}/leave`,
        method: 'post',
      });

      await Swal.fire({
        icon: 'success',
        title: '프로젝트 나가기 완료',
        text: '프로젝트에서 나갔습니다.',
        confirmButtonColor: '#1e50af'

      });

      // 페이지 새로고침 또는 프로젝트 목록 업데이트
      if (onProjectDeleted) {
        onProjectDeleted(projectKey);
      }

    } catch (err: any) {
      console.log("나가기 실패");
      await Swal.fire({
        icon: 'error',
        title: '프로젝트 나가기 실패',
        text: err.response?.data?.message || '프로젝트 나가기에 실패했습니다.',
        confirmButtonColor: '#1e50af'

      });
    }
  };

  return (
    <>
      {/* 프로젝트 카드 */}
      <div
        className={`w-72 rounded-[10px] border border-my-border inline-flex flex-col justify-start items-center overflow-hidden cursor-pointer transition-shadow hover:shadow-md relative ${className}`}
        onClick={handleCardClick}
        role="button"
        tabIndex={0} // 키보드 접근성 활성화
        onKeyDown={(e) => { // 엔터/스페이스 키로도 클릭 가능하게 설정
          if (e.key === 'Enter' || e.key === ' ') {
            handleCardClick();
          }
        }}
        {...rest}
      >

        {/* 2. 프로젝트 대표 이미지 */}
        {/* 이미지 컨테이너: h-52 고정 높이 */}
        <div className="self-stretch h-52 flex flex-col justify-center items-center overflow-hidden">
          {/* 3. 이미지 사이즈 조정 및 플레이스홀더 적용 */}
          {/* 부모 컨테이너(h-52)에 맞게 cover/contain을 사용하고 크기를 조정했습니다. */}
          <img
            // 💡 object-cover: 컨테이너에 맞게 이미지를 자르지 않고 채움 (대부분의 썸네일 카드에 적합)
            className="w-full h-full object-cover"
            src={imageUrl ? imageUrl : "https://placehold.co/500x320/E5E7EB/6B7280?text=No+Image"}
            alt={`${name} 이미지`}
          />
        </div>

        {/* 삭제버튼 */}
        {isOwner ? (<button className={`${deleteButtonHiddenClass} top-2 right-2 absolute`} onClick={handleDeleteClick}>
          <DeleteIcon />
        </button>
        ) : (
          <button className='top-2 right-2 absolute' onClick={handleLeaveProject}>
            {/* 나가기 */}
            <LeaveIcon />
          </button>
        )}
        {/* 프로젝트 정보 */}
        <div className="self-stretch px-4 py-3 border-t border-my-border flex flex-col justify-start items-start gap-2">

          {/* 제목 */}
          <div className="self-stretch h-6">
            <div className="text-my-black text-xl font-semibold overflow-hidden whitespace-nowrap text-ellipsis" title={name}>
              {/* 4. 불필요한 relative 제거 및 폰트 클래스 간소화 */}
              {name}
            </div>
          </div>

          {/* 프로젝트 마지막 수정일시 */}
          <div className="self-stretch flex justify-between items-center text-sm text-gray-500">
            <div className="font-normal">마지막 수정일</div>
            {/* <div className="font-medium">{updatedAt.getFullYear()}/{updatedAt.getMonth()}/{updatedAt.getDay()}</div> */}
            <div className="font-medium">{new Date(updatedAt).toLocaleDateString()}</div>
          </div>
        </div>

      </div>

      {/* 삭제 확인 모달 */}
      <DeleteProject
        projectKey={projectKeyToDelete}
        isOpen={isDeleteModalOpen}
        onClose={closeDeleteModal}
        onConfirm={confirmDelete}
      />
    </>
  );
};


export default ProjectCard;