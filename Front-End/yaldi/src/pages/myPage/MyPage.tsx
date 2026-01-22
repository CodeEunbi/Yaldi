import React, { useEffect, useState } from "react";
import EditIcon from '../../assets/icons/edit_icon.svg?react';
import AddIcon from '../../assets/icons/plus_icon.svg?react';
import ProjectCard from "../../components/common/ProjectCard";
import EditUserModal from "./EditUserModal";
import CreateTeamModal from '../team/CreateTeamModal';
import type { Team, TeamProject } from "../../types/teams";
import { useNavigate } from "react-router-dom";
import { apiController } from "../../apis/apiController";
import Swal from "sweetalert2";
import { useAuthStore } from "../../stores/authStore";

interface TeamItemProps {
  team: Team; // 컴포넌트가 받을 속성 정의
  onClick: () => void;
}

const TeamItem: React.FC<TeamItemProps> = ({ team, onClick }) => {
  return (
    <button
      onClick={onClick}
      className="flex rounded-lg w-full p-2 hover:bg-light-blue">
      {team.name}
    </button>
  );
}


const MyPage: React.FC = () => {

  const navigate = useNavigate();
  const user = useAuthStore((state) => state.currentUser);

  const [isEditUserModalOpen, setIsEditUserModalOpen] = useState(false);
  const [isCreateTeamModalOpen, setIsCreateTeamModalOpen] = useState(false);
  const [teams, setTeams] = useState<Team[] | null>(null);
  const [projects, setProjects] = useState<TeamProject[] | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);


  // 참여 프로젝트 목록 불러오기
  const getMyProjectList = async () => {
    try {
      const response = await apiController({
        url: '/api/v1/projects/my',
        method: 'get'
      });
      console.log("내 프로젝트 불러오기 성공", response.data);
      setProjects(response.data.result.data);
    } catch (err) {
      console.log("내 프로젝트 불러오기 에러", err);
      Swal.fire({
        text: "내 프로젝트 불러오기에 실패했습니다.",
        icon: "error",
        confirmButtonColor: '#1e50af',
      });
      // setProjects(dummyProjects);
      setProjects([]);
    }
  }

  // 참여 팀 목록 불러오기
  const getMyTeamList = async () => {
    try {
      const response = await apiController({
        url: '/api/v1/teams',
        method: 'get',
        params: {
          page: 0,
          size: 1000,
        }
      });
      console.log("내 팀 불러오기 성공", response.data);
      setTeams(response.data.result);
    } catch (err) {
      console.log("내 팀 불러오기 에러", err);
      Swal.fire({
        text: "내 팀 불러오기에 실패했습니다.",
        icon: "error",
        confirmButtonColor: '#1e50af',
      });
      setTeams([]);
    }
  };

  const navigateTeamPage = (teamKey: number) => {
    console.log(`${teamKey} 팀 페이지로 이동`);
    navigate(`/teampage/${teamKey}`);
  }

  const onCreateNewTeam = async (newTeamName: string) => {
    try {
      const response = await apiController({
        url: '/api/v1/teams',
        method: 'post',
        data: {
          name: newTeamName
        }
      });

      // 새로운 팀을 기존 목록에 추가
      if (teams) {
        setTeams([...teams, response.data.result]);
      } else {
        setTeams([response.data.result]);
      }

      Swal.fire({
        icon: 'success',
        text: '팀이 생성되었습니다.',
        timer: 2000,
        timerProgressBar: true,
        showConfirmButton: false,
      });
    } catch (err) {
      console.error("팀 생성 에러:", err);
      Swal.fire({
        icon: 'error',
        text: '팀 생성에 실패했습니다.',
        confirmButtonColor: '#1e50af',
      });
    }
  };


  const handleUserUpdate = (newNickname: string) => {
    // zustand store 업데이트
    if (user) {
      useAuthStore.setState({
        currentUser: {
          ...user,
          nickname: newNickname
        }
      });
    }

    Swal.fire({
      icon: 'success',
      text: '닉네임이 변경되었습니다.',
      timer: 2000,
      timerProgressBar: true,
      showConfirmButton: false,
    });
  };


  // 프로젝트 삭제
  const handleProjectDelete = async (projectKey: number) => {
    try {
      await apiController({
        url: `/api/v1/projects/${projectKey}`,
        method: 'delete'
      });

      // 프로젝트 목록에서 삭제된 항목 제거
      setProjects(prev =>
        prev ? prev.filter(p => p.projectKey !== projectKey) : null
      );

      Swal.fire({
        icon: 'success',
        text: '프로젝트가 삭제되었습니다.',
        timer: 2000,
        timerProgressBar: true,
        showConfirmButton: false,
      });
    } catch (err) {
      console.error("프로젝트 삭제 에러:", err);
      Swal.fire({
        icon: 'error',
        text: '프로젝트 삭제에 실패했습니다.',
        confirmButtonColor: '#1e50af',
      });
    }
  };


  useEffect(() => {
    const fetchData = async () => {
      try {
        await Promise.all([
          getMyProjectList(),
          getMyTeamList()
        ]);
      } catch (err) {
        console.error("데이터 로딩 실패:", err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, []);


  if (isLoading) {
    return (
      <div className="w-full h-dvh flex justify-center items-center">
        {/* 스피너 */}
        <div role="status">
          <svg aria-hidden="true" className="w-8 h-8 text-gray-200 animate-spin dark:text-gray-600 fill-blue-600" viewBox="0 0 100 101" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z" fill='#CFCFCF' />
            <path d="M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z" fill='#1E50AF' />
          </svg>
          <span className="sr-only">Loading...</span>
        </div>

      </div>
    );
  };

  if (!user) {
    return (
      <div className="w-full px-64 pt-36 text-my-black ">
        사용자 정보를 찾을 수 없습니다.
      </div>
    );
  };

  return (
    <div className="flex flex-col w-10/12  max-w-[1187.5px] justify-self-center justify-center content-center items-center py-7 gap-6 text-my-black">
      {/* 모달들 */}
      <EditUserModal
        isOpen={isEditUserModalOpen}
        onClose={() => setIsEditUserModalOpen(false)}
        onSuccess={handleUserUpdate}
        nickname={user.nickname}
      />
      <CreateTeamModal
        isOpen={isCreateTeamModalOpen}
        onClose={() => setIsCreateTeamModalOpen(false)}
        onCreate={onCreateNewTeam}
      />

      {/* 상단 블럭 */}
      <div className="flex flex-col w-full p-2 border border-my-border rounded-xl">
        {/* 제목 */}
        <div className="flex w-full items-center p-2 border-b border-my-border gap-2">
          <div className="w-auto text-xl font-bold">
            내 정보
          </div>
          <button
            onClick={() => setIsEditUserModalOpen((prev) => !prev)}
          >
            <EditIcon />
          </button>
        </div>
        {/* 상세정보 */}
        <div className="flex flex-col p-2">
          <div className="text-lg font-semibold">
            {user.nickname}
          </div>
          <div className="text-base">
            {user.email}
          </div>
        </div>
      </div>

      {/* 하단 구역 */}
      <div className="flex flex-col sm:flex-row w-full gap-5">

        {/* 왼쪽 블럭 */}
        <div className="flex flex-col w-full md:w-72  max-h-[667px] p-2 border border-my-border rounded-xl">
          {/* 제목 */}
          <div className="flex w-full items-center p-2 border-b border-my-border gap-2">
            <div className="w-auto text-xl font-bold">
              나의 팀
            </div>
            <button
              onClick={() => setIsCreateTeamModalOpen(true)}
            >
              <AddIcon />
            </button>
          </div>
          {/* 상세정보 */}
          {teams && <div className="pt-2 overflow-y-auto">
            {teams.length > 0 ? (
              teams.map((item) => (
                <TeamItem team={item} onClick={() => navigateTeamPage(item.teamKey)} key={item.teamKey} />
              ))) : (
              <div className="flex rounded-lg w-full p-2 text-gray-500">
                팀이 없습니다
              </div>
            )
            }
          </div>}
        </div>

        {/* 오른쪽 블럭 */}
        <div className="flex flex-col w-full max-h-[667px] p-2 border border-my-border rounded-xl">
          {/* 제목 */}
          <div className="flex w-full items-center p-2 border-b border-my-border gap-2">
            <div className="w-auto text-xl font-bold">
              나의 프로젝트
            </div>
          </div>

          {/* 세부내용 */}
          {projects && <div className="flex flex-wrap w-full p-2 gap-3 overflow-y-auto">
            {projects.length > 0 ? (
              projects.map((item) => (
                <ProjectCard
                  key={item.projectKey}
                  projectKey={item.projectKey}
                  name={item.name}
                  updatedAt={item.updatedAt}
                  imageUrl={item.imageUrl || undefined}
                  isOwner={item.role === "OWNER"}
                  onProjectDeleted={handleProjectDelete}
                />
              ))
            ) : (
              <div className="flex rounded-lg w-full p-2 text-gray-500">
                프로젝트가 없습니다
              </div>
            )}
          </div>}
        </div>
      </div>
    </div >
  );
};


export default MyPage;