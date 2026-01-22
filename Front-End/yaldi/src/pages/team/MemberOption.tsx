import React, { useEffect, useRef } from "react";
import type { TeamMember } from "../../types/teams";

interface memberOptionProps {
  isOpen: boolean;
  member: TeamMember;
  onClose: () => void;
  onRemove: (userKey: number) => void;
  onTransferOwnership: (userKey: number) => void;
  buttonRef?: React.RefObject<HTMLButtonElement | null>; // ✅ 버튼 위치 참조
}

const MemberOption: React.FC<memberOptionProps> = ({
  isOpen,
  member,
  onClose,
  onRemove,
  onTransferOwnership,
  buttonRef
}) => {
  const menuRef = useRef<HTMLDivElement>(null);

  // 메뉴 위치 계산
  useEffect(() => {
    if (!isOpen || !buttonRef?.current || !menuRef.current) return;

    const buttonRect = buttonRef.current.getBoundingClientRect();
    const menuElement = menuRef.current;

    // 버튼 아래에 메뉴 위치시키기
    menuElement.style.top = `${buttonRect.top}px`;
    menuElement.style.left = `${buttonRect.right}px`;
  }, [isOpen, buttonRef]);

  // 외부 클릭 감지
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    setTimeout(() => {
      document.addEventListener('mousedown', handleClickOutside);
    }, 0);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleRemove = () => {
    onRemove(member.userKey);
    onClose();
  };

  const handleTransferOwnership = () => {
    onTransferOwnership(member.userKey);
    onClose();
  };

  return (
    <div 
      ref={menuRef} 
      className="fixed flex flex-col rounded-[10px] bg-my-white border border-my-border shadow-lg z-50 overflow-hidden"
      // ↑ absolute → fixed로 변경
    >
      <button 
        className="flex w-full justify-center items-center p-2 border-b border-my-border hover:bg-light-blue whitespace-nowrap px-4"
        onClick={handleRemove}
      >
        팀에서 내보내기
      </button>
      <button 
        className="flex w-full justify-center items-center p-2 hover:bg-light-blue whitespace-nowrap px-4"
        onClick={handleTransferOwnership}
      >
        팀장 권한 위임하기
      </button>
    </div>
  );
};

export default MemberOption;