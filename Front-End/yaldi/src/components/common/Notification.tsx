
// const Nofitication: React.FC<NotificationProps> = ({
//   isOpen,
//   onClose, 
//   ...rest 
// }) => {

//   // 💡 팁: 모달이 열릴 때 body 스크롤을 막는 로직을 여기에 추가할 수 있습니다.
//   useEffect(() => {
//     if (isOpen) {
//       document.body.style.overflow = 'hidden';
//     } else {
//       document.body.style.overflow = 'unset';
//     }
//     return () => {
//       document.body.style.overflow = 'unset'; // 클린업 함수
//     };
//   }, [isOpen]);

//   if (!isOpen) {
//     return null;
//   }

//   // 오버레이 클릭 핸들러 (모달 본체가 아닌 바깥 영역 클릭 시 닫기)
//   const handleOverlayClick = (e: React.MouseEvent<HTMLDivElement>) => {
//     // 이벤트 버블링을 통해 오버레이 자체를 클릭했는지 확인 (모달 본체 클릭 제외)
//     if (e.target === e.currentTarget) {
//       onClose();
//     }
//   };

//   return (
//     <div
//       className="fixed inset-0 z-50"
//       onClick={handleOverlayClick}
//       {...rest}
//     >

//       <div className="top-20 right-4 absolute p-2.5 bg-my-white rounded-[10px] shadow-[0px_4px_8px_0px_rgba(0,0,0,0.20)] outline outline-1 outline-offset-[-1px] outline-border outline-my-border inline-flex flex-col justify-start items-center gap-1 overflow-hidden">
//         <div className="self-stretch h-10 p-2.5 rounded-[10px] inline-flex justify-start items-center gap-2.5">
//           <div data-svg-wrapper className="relative">
//             <InviteIcon />
//           </div>
//           <div className="w-72 justify-center"><span className="text-myBlack text-base font-bold font-['Pretendard']">김은비</span><span className="text-myBlack text-base font-normal font-['Pretendard']">님이 </span><span className="text-myBlack text-base font-bold font-['Pretendard']">Yaldi</span><span className="text-myBlack text-base font-normal font-['Pretendard']">에 초대했습니다.</span></div>
//         </div>
//         <div className="self-stretch h-10 p-2.5 rounded-[10px] inline-flex justify-start items-center gap-2.5">
//           <div data-svg-wrapper className="relative">
//             <NewVersionIcon />
//           </div>
//           <div className="justify-center"><span className="text-myBlack text-base font-bold font-['Pretendard']">공통PJT</span><span className="text-myBlack text-base font-normal font-['Pretendard']">에서 새로운 버전이 저장되었습니다.</span></div>
//         </div>
//         <div className="self-stretch h-10 p-2.5 rounded-[10px] inline-flex justify-start items-center gap-2.5">
//           <div data-svg-wrapper className="relative">
//             <OwnerIcon />
//           </div>
//           <div className="justify-center"><span className="text-myBlack text-base font-bold font-['Pretendard']">공통PJT</span><span className="text-myBlack text-base font-normal font-['Pretendard']">의 팀장이 </span><span className="text-myBlack text-base font-bold font-['Pretendard']">김은비</span><span className="text-myBlack text-base font-normal font-['Pretendard']">(으)로 변경되었습니다.</span></div>
//         </div>
//         <div className="self-stretch h-10 p-2.5 rounded-[10px] inline-flex justify-start items-center gap-2.5">
//           <div data-svg-wrapper className="relative">
//             <DeleteIcon />
//           </div>
//           <div className="justify-center"><span className="text-myBlack text-base font-bold font-['Pretendard']">공통PJT</span><span className="text-myBlack text-base font-normal font-['Pretendard']">가 삭제되었습니다.</span></div>
//         </div>
//         <div className="self-stretch h-10 p-2.5 rounded-[10px] inline-flex justify-start items-center gap-2.5">
//           <div data-svg-wrapper className="relative">
//             <AddMemberIcon />
//           </div>
//           <div className="justify-center"><span className="text-myBlack text-base font-bold font-['Pretendard']">공통PJT</span><span className="text-myBlack text-base font-normal font-['Pretendard']">에 추가되었습니다.</span></div>
//         </div>
//         <div className="self-stretch h-10 p-2.5 rounded-[10px] inline-flex justify-start items-center gap-2.5">
//           <div data-svg-wrapper className="relative">
//             <RemoveMemberIcon />
//           </div>
//           <div className="justify-center"><span className="text-myBlack text-base font-bold font-['Pretendard']">공통PJT</span><span className="text-myBlack text-base font-normal font-['Pretendard']">에서 제외되었습니다.</span></div>
//         </div>
//       </div>
//     </div>
//   );
// };

// export default Nofitication;


import React, { useEffect, useMemo } from 'react';
// SVG 아이콘 컴포넌트 임포트 (Vite/svgr 사용 가정)
import OwnerIcon from '../../assets/icons/owner_icon.svg?react';
import InviteIcon from '../../assets/icons/mail_icon.svg?react';
import NewVersionIcon from '../../assets/icons/new_version_icon.svg?react';
import AddMemberIcon from '../../assets/icons/add_member_iconsvg.svg?react';
import RemoveMemberIcon from '../../assets/icons/remove_member_icon.svg?react';
import DeleteIcon from '../../assets/icons/delete_icon.svg?react';
import MoreIcon from '../../assets/icons/more_icon.svg?react';

// 알림 코드와 내용 정의
type NotificationCode =
  | 'TEAM_INVITE'
  | 'VERSION_UPDATED'
  | 'TEAM_OWNER_CHANGED'
  | 'PROJECT_OWNER_CHANGED'
  | 'PROJECT_REMOVED'
  | 'PROJECT_ADD_MEMBER'
  | 'TEAM_MEMBER_REMOVED'
  | 'UNKNOWN';

export interface NotificationItem {
  id: string; // 각 알림 항목의 고유 ID
  type: NotificationCode; // 알림의 종류를 나타내는 코드
  content: string; // 알림 본문 내용
  readAt?: string; // 읽음 여부 확인용
}

interface NotificationProps extends React.ComponentPropsWithoutRef<'div'> {
  isOpen: boolean;
  onClose: () => void;
  // 알림 목록 Prop 추가
  notifications: NotificationItem[];
  // 알림 항목을 클릭했을 때 처리하는 함수 (예: 읽음 처리, 관련 페이지 이동)
  onNotificationClick: (id: string, code: NotificationCode) => void;
  // 전체 읽음 처리 함수 (선택적)
  onMarkAllAsRead?: () => void;
}


// 알림 종류에 따라 아이콘과 메시지 구조를 매핑하는 데이터
const NOTIFICATION_MAPPING: Record<NotificationCode, { icon: React.FC<React.SVGProps<SVGSVGElement>>; }> = {
  TEAM_INVITE: {
    icon: InviteIcon,
  },
  VERSION_UPDATED: {
    icon: NewVersionIcon,
  },
  TEAM_OWNER_CHANGED: {
    icon: OwnerIcon,
  },
  PROJECT_OWNER_CHANGED: {
    icon: OwnerIcon,
  },
  PROJECT_REMOVED: {
    icon: DeleteIcon,
  },
  PROJECT_ADD_MEMBER: {
    icon: AddMemberIcon,
  },
  TEAM_MEMBER_REMOVED: {
    icon: RemoveMemberIcon,
  },
  UNKNOWN: {
    icon: MoreIcon, // 기본 아이콘 (수정 필요)
  },
};

// 개별 알림 항목 컴포넌트
interface NotificationItemProps extends NotificationItem {
  onClick: (id: string, type: NotificationCode) => void;
}

const NotificationItemComponent: React.FC<NotificationItemProps> = ({
  id,
  type,
  content,
  onClick
}) => {
  const mapping = NOTIFICATION_MAPPING[type] || NOTIFICATION_MAPPING.UNKNOWN;
  const IconComponent = mapping.icon;

  return (
    <div
      className='self-stretch p-2.5 rounded-lg inline-flex justify-start items-center gap-2 cursor-pointer transition-colors'
      onClick={() => onClick(id, type)}
    >
      {/* 아이콘 컨테이너 */}
      <div className="flex-shrink-0 w-6 h-6 text-my-black">
        <IconComponent className="w-full h-full fill-current" />
      </div>

      {/* 텍스트 내용 */}
      {/* 폰트 클래스 간소화 및 텍스트 줄바꿈 방지/ellipsis 추가 */}
      <div className="flex-grow text-sm text-my-black font-['Pretendard'] truncate">
        {content}
      </div>
    </div>
  );
};


const Notification: React.FC<NotificationProps> = ({
  isOpen,
  onClose,
  notifications,
  onNotificationClick,
  onMarkAllAsRead,
  ...rest
}) => {

  const hasUnread = useMemo(() => notifications.some(n => !n.readAt), [notifications]);


  // 🚀 화면 흔들림 방지 및 스크롤바 제어 로직 수정
  useEffect(() => {
    if (typeof window === 'undefined') return;

    const bodyStyle = document.body.style;
    const originalPaddingRight = bodyStyle.paddingRight;
    const originalOverflow = bodyStyle.overflow;

    if (isOpen) {
      // 1. 스크롤바 너비 계산: 뷰포트 너비 - HTML 문서 너비
      const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
      // 2. 스크롤 숨김 및 보정 패딩 적용
      bodyStyle.overflow = 'hidden';
      bodyStyle.paddingRight = `${scrollbarWidth}px`;
    }

    // 클린업 함수: 닫힐 때 또는 언마운트 시 원래 상태로 복원
    return () => {
      bodyStyle.overflow = originalOverflow;
      bodyStyle.paddingRight = originalPaddingRight;
    };
  }, [isOpen]); // isOpen 상태가 변할 때마다 실행


  // Body 스크롤 막기 로직
  useEffect(() => {
    const bodyStyle = document.body.style;
    if (isOpen) {
      bodyStyle.overflow = 'hidden';
    } else {
      bodyStyle.overflow = 'unset';
    }
    return () => {
      bodyStyle.overflow = 'unset';
    };
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  // 알림 본체 클릭 시 오버레이로 이벤트 전파 방지
  const handleBodyClick = (e: React.MouseEvent<HTMLDivElement>) => {
    e.stopPropagation();
  };

  return (
    // 오버레이 영역: 전체 화면을 덮고, 클릭 시 닫힘
    <div
      // 배경을 투명하게 (backdrop-blur-sm은 제거) 하여 헤더 뒤쪽이 자연스럽게 보이도록 설정
      className="fixed inset-0 z-50 bg-transparent"
      onClick={onClose} // 오버레이 클릭 시 닫기
      {...rest}
    >
      {/* 알림 본체: 헤더 오른쪽 버튼 위치에 맞게 조정 */}
      <div
        className="top-[80px] right-6 absolute p-4 bg-my-white rounded-xl shadow-2xl border border-my-border w-96 max-h-[calc(100vh-100px)] flex flex-col overflow-hidden"
        onClick={handleBodyClick} // 알림 본체 클릭 이벤트 전파 방지
      >
        {/* 헤더 */}
        <div className="flex justify-between items-center pb-2 border-b border-my-border mb-2">
          <h3 className="text-lg font-bold text-my-black">알림</h3>
          {/* 전체 읽음 버튼 (읽지 않은 알림이 있을 때만 표시) */}
          {hasUnread && onMarkAllAsRead && (
            <button
              onClick={onMarkAllAsRead}
              className="text-blue-600 hover:text-blue-700 text-sm font-medium transition-colors inline-flex items-center gap-1 p-1 rounded"
            >
              모두 읽음
            </button>
          )}
        </div>

        {/* 알림 목록 컨테이너 */}
        <div className="flex-grow overflow-y-auto space-y-1">
          {notifications.length > 0 ? (
            notifications.map((item) => (
              <NotificationItemComponent
                key={item.id}
                {...item}
                onClick={onNotificationClick}
              />
            ))
          ) : (
            <div className="text-center py-8 text-my-black">새로운 알림이 없습니다.</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Notification;