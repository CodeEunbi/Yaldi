import React, { useEffect, useRef } from "react";

interface invitationOptionProps {
  isOpen: boolean;
  invitationKey: number;
  onClose: () => void;
  onCancelInvite: (invitationKey: number) => void;
  buttonRef?: React.RefObject<HTMLButtonElement | null>; // ✅ 버튼 위치 참조
}

const InvitationOption: React.FC<invitationOptionProps> = ({
  isOpen,
  invitationKey,
  onClose,
  onCancelInvite,
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

  const handleCancelInvitation = () => {
    onCancelInvite(invitationKey);
    onClose();
  };


  return (
    <div
      ref={menuRef}
      className="fixed flex flex-col rounded-[10px] bg-my-white border border-my-border shadow-lg z-50 overflow-hidden"
    // ↑ absolute → fixed로 변경
    >
      <button
        className="flex w-full justify-center items-center p-2 hover:bg-light-blue whitespace-nowrap px-4"
        onClick={handleCancelInvitation}
      >
        초대 취소하기
      </button>
    </div>
  );
};

export default InvitationOption;