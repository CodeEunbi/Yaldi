import React, { useCallback, useEffect, useState } from "react";
import ModalSmall from "../../components/common/ModalSmall";
import FilledButton from "../../components/common/FilledButton";
import InputBox from "../../components/common/InputBox";
import { useUserSearch, type SearchedUser } from '../../hooks/useUserSearch';
import { useInviteMember } from '../../hooks/useInviteTeamMember';
import { type Invitations } from "../../types/teams";


interface InviteMemberProps {
  isOpen: boolean;
  teamKey: number;
  onClose: () => void;
  onSuccess: (newInvitation: Invitations) => void; // ← 성공 시 상위에 알림
}


const InviteMember: React.FC<InviteMemberProps> = ({
  isOpen,
  teamKey,
  onClose,
  onSuccess
}) => {


  const [searchKeyword, setSearchKeyword] = useState("");
  const [selectedUser, setSelectedUser] = useState<SearchedUser | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Hooks 사용
  const {
    searchResults,
    isSearching,
    searchError,
    searchUsers,
    clearSearch,
  } = useUserSearch();

  const {
    inviteMember,
    isInviting,
    // inviteError,
  } = useInviteMember();

  // 모달이 열릴 때 초기화
  useEffect(() => {
    if (isOpen) {
      setSearchKeyword("");
      setSelectedUser(null);
      setErrorMessage(null);
      clearSearch();
    }
  }, [isOpen, clearSearch]);

  // 검색 실행
  const handleSearch = useCallback(async () => {
    await searchUsers(searchKeyword, teamKey);
  }, [searchKeyword, searchUsers]);

  // Enter 키로 검색
  const handleSearchKeyDown = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSearch();
    }
  }, [handleSearch]);

  // 사용자 선택
  const handleSelectUser = useCallback((user: SearchedUser) => {
    setSelectedUser(user);
    setErrorMessage(null);
  }, []);


  // 초대하기
  const handleInvite = useCallback(async () => {
    if (!selectedUser) {
      setErrorMessage("초대할 사용자를 선택해주세요.");
      return;
    }

    const isExisting = selectedUser.status === "ALREADY_MEMBER";
    if (isExisting) {
      setErrorMessage("이미 팀에 속한 멤버입니다.");
      return;
    }

    try {
      const newInvitation = await inviteMember(teamKey, selectedUser.userKey);
      onSuccess(newInvitation);
      onClose();
    } catch (error) {
      // 에러는 hook에서 처리했지만, UI에 표시
      setErrorMessage((error as Error).message);
    }
  }, [selectedUser, teamKey, inviteMember, onSuccess, onClose]);

  const handleCloseModal = useCallback(() => {
    setSearchKeyword("");
    setSelectedUser(null);
    setErrorMessage(null);
    clearSearch();
    onClose();
  }, [clearSearch, onClose]);


  return (
    <ModalSmall isOpen={isOpen} onClose={handleCloseModal} title="팀원 추가">
      <div className='flex w-full flex-col items-center justify-between gap-4 p-2.5'>
        <div className='flex w-full text-my-black items-start justify-between gap-2'>
          <div className="flex-shrink-0 text-lg font-medium w-auto mt-3">
            유저 검색
          </div>
          <div className="flex flex-col flex-grow justify-start ">
            <div className="flex w-full gap-2 items-center">

              <InputBox
                placeholder="닉네임 또는 이메일을 입력하세요"
                className="flex-"
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyDown={handleSearchKeyDown}
                maxLength={20}
                disabled={isSearching || isInviting}
                size="flex-grow p-2.5"
              />
              <FilledButton
                label="검색"
                onClick={handleSearch}
                disabled={isSearching || isInviting || searchKeyword.trim().length < 2}
              />
            </div>
            {searchError && (
              <div className="text-sm text-red-500 font-medium">
                * {searchError}
              </div>
            )}
          </div>

        </div>

        {/* 검색 결과 영역 */}
        {searchResults !== null && (
          <div className='flex w-full flex-col gap-2'>
            {searchResults.length > 0 ? (
              <div className="flex flex-col gap-2 max-h-48 overflow-y-auto border border-gray-200 rounded-lg p-2">
                {searchResults.map(user => (
                  <button
                    key={user.userKey}
                    onClick={() => handleSelectUser(user)}
                    disabled={isInviting || user.status !== "INVITABLE"}
                    className={`
                      flex items-center gap-3 p-3 rounded-lg text-left transition-colors
                      ${selectedUser?.userKey === user.userKey
                        ? 'bg-blue-100 border-2 border-blue-500'
                        : 'bg-gray-50 hover:bg-gray-100 border-2 border-transparent'
                      }
                      disabled:opacity-50
                      `}
                  >
                    <div className="w-10 h-10 rounded-full bg-gray-300 flex items-center justify-center text-white font-bold">
                      {user.nickname.charAt(0)}
                    </div>
                    <div className="flex-1">
                      <div className="font-medium">{user.nickname}</div>
                      <div className="text-sm text-gray-500">{user.email}</div>
                    </div>
                    {selectedUser?.userKey === user.userKey && (
                      <div className="text-blue-500 font-bold">✓</div>
                    )}
                  </button>
                ))}
              </div>
            ) : (
              <div className="text-center text-gray-500 py-4">
                검색 결과가 없습니다
              </div>
            )}
          </div>
        )}


        {/* 에러 메시지 */}
        {errorMessage && (
          <div className="w-full text-sm text-red-500 font-medium">
            * {errorMessage}
          </div>
        )}


        <div className='flex w-full justify-end items-center gap-4'>
          <FilledButton
            label="초대하기"
            onClick={handleInvite}
            disabled={!selectedUser || isInviting}
          />
        </div>
      </div>
    </ModalSmall>

  )
}

export default InviteMember;