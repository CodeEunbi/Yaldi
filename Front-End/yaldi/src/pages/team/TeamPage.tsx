import React, { useEffect, useRef, useState } from "react";

import EditIcon from '../../assets/icons/edit_icon.svg?react';
import AddIcon from '../../assets/icons/plus_icon.svg?react';
import OwnerIcon from '../../assets/icons/owner_icon.svg?react';
import MailIcon from '../../assets/icons/mail_icon.svg?react';
import ProjectCard from "../../components/common/ProjectCard";
import LeaveIcon from "../../assets/icons/logout_icon.svg?react";

import type { Team, TeamProject, TeamMember, ModalState, Invitations } from "../../types/teams";

import InviteMember from "./InviteMember";
import CreateProject from "../project/CreateProjectModal";
import EditTeam from "./EditTeam";
import RemoveMember from "./RemoveMember";
import TransferOwnership from "./TransferOwnership";
import MemberOption from "./MemberOption";
import InvitationOption from './InvitationOption';
import DeleteTeam from './DeleteTeam';
import { useParams, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import MyNavigation from "../../utils/MyNavigation";
import { apiController } from "../../apis/apiController";
import Swal from "sweetalert2";
import type { ApiError } from "../../types/api";


interface MemberItemProps {
  member: TeamMember; // 컴포넌트가 받을 속성 정의
  isOwner: boolean;
  isMe: boolean;
  isOpenOption: boolean;
  onCloseOption: () => void;
  onClick: () => void;
  onRemove: (userKey: number) => void;
  onTransferOwnership: (userKey: number) => void;
}

const MemberItem: React.FC<MemberItemProps> = ({
  member,
  isOwner,
  isMe,
  isOpenOption,
  onCloseOption,
  onClick,
  onRemove,
  onTransferOwnership,
}) => {

  const buttonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <button
        onClick={onClick}
        ref={buttonRef} // ✅ ref 연결
        className="relative flex rounded-lg w-full p-2 hover:bg-light-blue gap-1"
        disabled={isMe}
      >
        {member.nickName}
        {isMe && "(나)"}
        {isOwner && <OwnerIcon />}
      </button>
      <MemberOption isOpen={isOpenOption} member={member} onClose={onCloseOption} onRemove={onRemove}
        onTransferOwnership={onTransferOwnership} buttonRef={buttonRef} />
    </>
  );
}

interface InvitationItemProps {
  invitation: Invitations | null; // 컴포넌트가 받을 속성 정의
  isOpenOption: boolean;
  onCloseOption: () => void;
  onClick: () => void;
  onCancelInvite: (invitationKey: number) => void;
}

const InvitationItem: React.FC<InvitationItemProps> = ({
  invitation,
  isOpenOption,
  onCloseOption,
  onClick,
  onCancelInvite,
}) => {

  const buttonRef = useRef<HTMLButtonElement>(null);

  if (!invitation) return;

  return (
    <>
      <button
        onClick={onClick}
        ref={buttonRef} // ✅ ref 연결
        className="relative flex rounded-lg w-full p-2 hover:bg-light-blue gap-1"
      >
        {invitation.invitedNickname}
        <MailIcon />
      </button>
      <InvitationOption isOpen={isOpenOption} invitationKey={invitation.invitationKey} onClose={onCloseOption} onCancelInvite={onCancelInvite}
        buttonRef={buttonRef} />
    </>
  );
}



const TeamPage: React.FC = () => {

  const navigate = useNavigate();
  const currentTeamKey = Number(useParams().teamKey);
  const user = useAuthStore((state) => state.currentUser);

  if (!user) {
    MyNavigation.goToLogin();
    return;
  }


  const [team, setTeam] = useState<Team | null>(null);
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [invitations, setInvitations] = useState<Invitations[]>([]);
  const [projects, setProjects] = useState<TeamProject[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [modals, setModals] = useState<ModalState>({
    editTeam: false,
    deleteTeam: false,
    inviteMember: false,
    removeMember: { isOpen: false, userKey: null },
    transferOwnership: { isOpen: false, userKey: null },
    createProject: false,
    deleteProject: { isOpen: false, projectKey: null },
    memberOption: { isOpen: false, userKey: null },
    invitationOption: { isOpen: false, invitationKey: null },
  })

  // 팀 이름 조회.
  const getTeamInfo = async () => {
    try {
      const response = await apiController({
        url: `/api/v1/teams/${currentTeamKey}`,
        method: 'get'
      });

      console.log("팀 이름 조회 성공");
      setTeam(response.data.result);

    } catch (err) {

      console.error("팀 정보 조회 실패:", err);

      const error = err as ApiError;

      if (error.status === 403) {
        Swal.fire({
          icon: 'warning',
          text: "이 팀에 권한이 없습니다.",
          confirmButtonColor: '#1e50af',
        })
        navigate("/mypage", { replace: true });
        return;
      }

      Swal.fire({
        icon: 'error',
        title: '팀 정보 조회 실패',
        text: '팀 정보를 불러올 수 없습니다.',
        confirmButtonColor: '#1e50af',
      });
      navigate('/mypage');
    }
  };

  const getTeamMembers = async () => {
    try {
      const response = await apiController({
        url: `/api/v1/teams/${currentTeamKey}/members?page=0&size=100`,
        method: 'get'
      });
      console.log("팀 멤버 목록 조회 성공", response);
      setMembers(response.data.result || []);
    } catch (error: any) {
      console.error("팀 멤버 조회 실패:", error);
      setMembers([]);
    }
  };

  const getTeamProjects = async () => {
    try {
      const response = await apiController({
        url: `/api/v1/projects/team/${currentTeamKey}`,
        method: 'get'
      });
      console.log("프로젝트 목록 조회 성공");
      setProjects(response.data.result.data)
    } catch (error: any) {
      console.error("프로젝트 조회 실패:", error);
      setProjects([]);
    }
  };

  const getPendingInvitations = async () => {

    if (!isThisUserOwner) return;

    try {
      const response = await apiController({
        url: `/api/v1/teams/${currentTeamKey}/invitations/pending`,
        method: 'get'
      });
      console.log("초대 목록 조회 성공");
      setInvitations(response.data.result.invitations || []);
    } catch (error: any) {
      console.error("초대 목록 조회 실패:", error);
      setInvitations([]);
    }
  };




  const isThisUserOwner = team ? user.userKey === team.ownedBy : false;


  // 모달 열기/닫기 헬퍼 함수
  const openModal = (modalName: keyof ModalState,
    data?:
      | boolean
      | { isOpen: boolean; userKey: number }
      | { isOpen: boolean; projectKey: number }
      | { isOpen: boolean; invitationKey: number }

  ) => {
    setModals(prev => ({
      ...prev,
      [modalName]: data || true
    }));
  };


  const closeModal = (modalName: keyof ModalState) => {
    setModals(prev => ({
      ...prev,
      [modalName]: modalName === 'deleteProject'
        ? { isOpen: false, projectKey: null }
        : modalName === 'removeMember'
          ? { isOpen: false, userKey: null }
          : modalName === 'transferOwnership'
            ? { isOpen: false, userKey: null }
            : modalName === 'memberOption'
              ? { isOpen: false, userKey: null }
              : modalName === 'invitationOption'
                ? { isOpen: false, invitationKey: null }
                : false
    }));
  };


  const onClickEditTeam = () => {
    if (!isThisUserOwner) return;
    openModal("editTeam");
  }


  const handleEditTeamSuccess = async (newTeam: Team) => {
    setTeam(newTeam);
    console.log("팀 이름 수정 완료");
  }


  const onClickInviteMember = () => {
    if (!isThisUserOwner) return;
    openModal("inviteMember");
  }


  // 초대 성공 콜백
  const handleMemberInvited = async (newInvitation: Invitations) => {
    // 초대 목록 새로고침
    await getPendingInvitations();

    await Swal.fire({
      icon: 'success',
      title: '초대 성공',
      text: `${newInvitation.invitedNickname}님을 팀에 초대했습니다.`,
      confirmButtonColor: '#1e50af',
    });

  };


  const onClickMember = (userKey: number) => {
    if (!isThisUserOwner) return;
    openModal("memberOption", { isOpen: true, userKey: userKey });
  }


  const onClickRemoveMember = (userKey: number) => {
    if (!isThisUserOwner) return;
    console.log(`${userKey} 팀원 제거 시도`);
    openModal("removeMember", { isOpen: true, userKey: userKey });
  }

  const handleLeaveTeam = async () => {
    try {
      await apiController({
        url: `/api/v1/teams/${currentTeamKey}/members/me`,
        method: 'delete',
      })

      console.log("팀 나가기 성공");
      navigate("/mypage", { replace: true });

    } catch (err: any) {
      console.log("팀 나가기 실패", err);

      await Swal.fire({
        icon: 'error',
        title: '팀 나가기 실패',
        text: err.response?.data?.message || '팀 나가기에 실패했습니다.',
        confirmButtonColor: '#1e50af',
      });
    };
  };

  const handleConformRemove = async (userKey: number | null) => {
    if (!userKey) return;

    try {
      await apiController({
        url: `/api/v1/teams/${currentTeamKey}/members/${userKey}`,
        method: 'delete'
      });

      console.log(`${userKey} 내보내기`);
      setMembers(prev => prev.filter(m => m.userKey !== userKey));

      await Swal.fire({
        icon: 'success',
        title: '멤버 제거 완료',
        text: '팀 멤버를 제거했습니다.',
        confirmButtonColor: '#1e50af',
      });
    } catch (error: any) {
      console.error("멤버 제거 실패:", error);
      await Swal.fire({
        icon: 'error',
        title: '멤버 제거 실패',
        text: error.response?.data?.message || '멤버 제거에 실패했습니다.',
        confirmButtonColor: '#1e50af',
      });
    }
  }


  const onClickTransferOwnership = (userKey: number) => {
    if (!isThisUserOwner) return;
    console.log(`${userKey} 에게 권한 넘길건가요?`);
    openModal("transferOwnership", { isOpen: true, userKey: userKey });
  }


  const handleConfirmTransferOwnership = async (userKey: number | null) => {
    if (!userKey) return;

    try {
      await apiController({
        url: `/api/v1/teams/${currentTeamKey}/owner`,
        method: 'patch',
        data: { newOwnerUserKey: userKey }
      });

      console.log(`${userKey} 에게 권한 넘기기`);
      setTeam(prev => ({ ...prev!, ownedBy: userKey }));

      await Swal.fire({
        icon: 'success',
        title: '권한 이전 완료',
        text: '팀 오너 권한을 이전했습니다.',
        confirmButtonColor: '#1e50af',
      });
    } catch (error: any) {
      console.error("권한 이전 실패:", error);
      await Swal.fire({
        icon: 'error',
        title: '권한 이전 실패',
        text: error.response?.data?.message || '권한 이전에 실패했습니다.',
        confirmButtonColor: '#1e50af',
      });
    }
  }


  const onClickInvitation = (invitationKey: number) => {
    if (!isThisUserOwner) return;
    openModal("invitationOption", { isOpen: true, invitationKey: invitationKey });

  }


  const onClickCancleInvitation = async (invitationKey: number) => {
    if (!isThisUserOwner) return;

    try {
      await apiController({
        url: `/api/v1/teams/invitations/${invitationKey}`,
        method: 'delete'
      });

      console.log(`${invitationKey}번 초대 취소함`);
      setInvitations(prev => prev.filter(i => i.invitationKey !== invitationKey));

      await Swal.fire({
        icon: 'success',
        title: '초대 취소 완료',
        text: '초대를 취소했습니다.',
        confirmButtonColor: '#1e50af',
      });
    } catch (error: any) {
      console.error("초대 취소 실패:", error);
      await Swal.fire({
        icon: 'error',
        title: '초대 취소 실패',
        text: error.response?.data?.message || '초대 취소에 실패했습니다.',
        confirmButtonColor: '#1e50af',
      });
    }
  }


  const onClickCreateProject = () => {
    openModal("createProject");
  }


  const onClickDeleteTeam = () => {
    if (!isThisUserOwner) return;
    openModal("deleteTeam");
  }


  const handleConfirmDeleteTeam = async () => {
    if (!team) return;

    try {
      await apiController({
        url: `/api/v1/teams/${currentTeamKey}`,
        method: 'delete'
      });

      console.log(`${team.teamKey} 팀 삭제`);

      await Swal.fire({
        icon: 'success',
        title: '팀 삭제 완료',
        text: '팀이 삭제되었습니다.',
        confirmButtonColor: '#1e50af',
      });

      navigate("/mypage");
    } catch (error: any) {
      console.error("팀 삭제 실패:", error);
      await Swal.fire({
        icon: 'error',
        title: '팀 삭제 실패',
        text: error.response?.data?.message || '팀 삭제에 실패했습니다.',
        confirmButtonColor: '#1e50af',
      });
      closeModal("deleteTeam");
    }
  }


  useEffect(() => {
    const fetchData = async () => {
      setIsLoading(true);
      await Promise.all([
        getTeamInfo(),
        getTeamMembers(),
        getTeamProjects(),
        getPendingInvitations()
      ]);
      setIsLoading(false);
    };

    fetchData();
  }, [currentTeamKey])

  // 로딩 중이거나 팀 정보가 없을 때
  if (isLoading || !team) {
    return (
      <div className="flex flex-col w-10/12 max-w-[1187.5px] justify-self-center justify-center content-center items-center py-7 gap-6 text-my-black">
        <div className="flex justify-center items-center h-screen">
          <div className="text-xl">로딩 중...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col w-10/12  max-w-[1187.5px] justify-self-center justify-center content-center items-center py-7 gap-6 text-my-black">

      {/* 모달들 */}
      <InviteMember
        isOpen={modals.inviteMember}
        teamKey={team.teamKey}
        onClose={() => closeModal('inviteMember')}
        onSuccess={handleMemberInvited}
      />
      <CreateProject
        userKey={user.userKey}
        teamKey={team.teamKey}
        teamMembers={members}
        isOpen={modals.createProject}
        onClose={() => closeModal('createProject')}
      />
      <EditTeam
        isOpen={modals.editTeam}
        onClose={() => closeModal("editTeam")}
        team={team}
        onSuccess={handleEditTeamSuccess}
        onDeleteClick={onClickDeleteTeam}
      />
      <RemoveMember
        isOpen={modals.removeMember.isOpen}
        onClose={() => closeModal("removeMember")}
        userKey={modals.removeMember.userKey}
        onConfirm={handleConformRemove}
      />
      <TransferOwnership
        isOpen={modals.transferOwnership.isOpen}
        onClose={() => closeModal("transferOwnership")}
        userKey={modals.transferOwnership.userKey}
        onConfirm={handleConfirmTransferOwnership}
      />
      <DeleteTeam
        teamKey={team.teamKey}
        isOpen={modals.deleteTeam}
        onClose={() => closeModal("deleteTeam")}
        onConfirm={handleConfirmDeleteTeam}
      />

      {/* 상단 블럭 */}
      <div className="flex flex-col w-full p-2 border border-my-border rounded-xl">
        {/* 제목 */}
        <div className="flex w-full items-center p-2 gap-1">
          <div className="w-auto text-2xl font-bold">
            {team.name}
          </div>
          {isThisUserOwner ?
            <button
              onClick={onClickEditTeam}
            >
              <EditIcon />
            </button> :
            <button
              onClick={handleLeaveTeam}>
              <LeaveIcon />
            </button>}
        </div>
      </div>

      {/* 하단 구역 */}
      <div className="flex flex-col sm:flex-row w-full gap-5">

        {/* 왼쪽 블럭 */}
        <div className="flex flex-col w-full md:w-72  max-h-[667px] p-2 border border-my-border rounded-xl">
          {/* 제목 */}
          <div className="flex w-full items-center p-2 border-b border-my-border gap-2">
            <div className="w-auto text-xl font-bold">
              멤버
            </div>
            {isThisUserOwner && <button
              onClick={onClickInviteMember}
            >
              <AddIcon />
            </button>
            }
          </div>
          {/* 상세정보 */}
          <div className="pt-2 overflow-y-auto">
            {members.length > 0 ? (
              members.map((item) => (
                <MemberItem member={item} isOwner={team.ownedBy === item.userKey} onClick={() => onClickMember(item.userKey)} isOpenOption={modals.memberOption.isOpen && modals.memberOption.userKey === item.userKey
                } onCloseOption={() => closeModal("memberOption")} key={item.userKey} isMe={item.userKey === user.userKey} onRemove={onClickRemoveMember} onTransferOwnership={onClickTransferOwnership} />
              ))) : (
              <div className="flex rounded-lg w-full p-2 text-gray-500">
                멤버가 없습니다
              </div>
            )
            }
            {isThisUserOwner && invitations.map((item) => (
              // 초대 중인 사람 목록.
              <InvitationItem key={item.invitationKey} invitation={item} onClick={() => onClickInvitation(item.invitationKey)}
                isOpenOption={modals.invitationOption.isOpen && modals.invitationOption.invitationKey === item.invitationKey}
                onCloseOption={() => closeModal("invitationOption")} onCancelInvite={onClickCancleInvitation}
              />
            ))}
          </div>
        </div>

        {/* 오른쪽 블럭 */}
        <div className="flex flex-col w-full max-h-[667px] p-2 border border-my-border rounded-xl">
          {/* 제목 */}
          <div className="flex w-full items-center p-2 border-b border-my-border gap-2">
            <div className="w-auto text-xl font-bold">
              팀 프로젝트
            </div>
            <button
              onClick={onClickCreateProject}
            >
              <AddIcon />
            </button>
          </div>

          {/* 세부내용 */}
          <div className="flex flex-wrap w-full p-2 gap-3 overflow-y-auto">
            {projects.length > 0 ? (
              projects.map((item) => (
                <ProjectCard
                  key={item.projectKey}
                  projectKey={item.projectKey}
                  name={item.name}
                  updatedAt={item.updatedAt}
                  imageUrl={item.imageUrl || undefined}
                  isOwner={item.role === "OWNER"}
                  onProjectDeleted={(projectKey) => {
                    setProjects(prev => prev.filter(p => p.projectKey !== projectKey));
                  }}
                />
              ))
            ) : (
              <div className="flex rounded-lg w-full p-2 text-gray-500">
                프로젝트가 없습니다
              </div>
            )}
          </div>
        </div>
      </div>
    </div >
  );
};


export default TeamPage;