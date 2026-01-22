import React from 'react';
import Logo from '../../assets/images/yaldi-logo.svg?react';
import LogoutIcon from '../../assets/icons/logout_icon.svg?react';
import NotificationIcon from '../../assets/icons/notification_icon.svg?react';
import MyPageIcon from '../../assets/icons/my_page_icon.svg?react';
import SearchIcon from '../../assets/icons/search_icon.svg?react';
import FilledButton from './FilledButton';
import { useNavigate } from 'react-router-dom';
import { useSearchQuery } from '../../hooks/useSearchQuery';
import { useAuthStore } from '../../stores/authStore';
import { useNotificationStore } from '../../stores/notificationStore';


interface CenterSearchInputProps {
  searchTerm: string;
  onSearchChange: (term: string) => void;
  onSearchSubmit: () => void;
}

// 💡 별도의 검색 컴포넌트 정의 (중앙 입력 필드 역할)
const CenterSearchInput: React.FC<CenterSearchInputProps> = ({ searchTerm, onSearchChange, onSearchSubmit }) => {

  // 엔터 키 처리
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      onSearchSubmit();
      // 포커스 해제 (Blur)
      e.currentTarget.blur();
    }
  };

  return (
    <div className="flex w-full max-w-lg min-w-24 items-center relative mx-4">
      <input
        type="text"
        value={searchTerm}
        onChange={(e) => onSearchChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="검색"
        className="w-full h-10 px-4 pl-10 text-base text-gray-700 placeholder-gray-400 bg-gray-100 border border-gray-200 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
        aria-label="검색 입력 필드"
      />
      {/* 검색 아이콘 (입력 필드 내부에 고정) */}
      <button
        onClick={onSearchSubmit}
        className="absolute left-2 p-1 text-gray-400 hover:text-gray-600 transition-colors"
        aria-label="검색 실행"
      >
        <SearchIcon className="w-5 h-5 fill-current" />
      </button>
    </div>
  );
};

const MainHeader: React.FC = () => {

  // ⭐ 알림 상태 구독
  const hasUnread = useNotificationStore((state) => state.hasUnread);
  // const unreadCount = useNotificationStore((state) => state.unreadCount);
  const markAllAsRead = useNotificationStore((state) => state.markAllAsRead);

  const isAuthenticated = useAuthStore((state) => state.isLoggedIn);
  const logout = useAuthStore((state) => state.logout);

  const navigate = useNavigate();
  const { query, setQuery } = useSearchQuery();

  const [searchTerm, setSearchTerm] = React.useState<string>(query);


  // URL 쿼리와 로컬 state 동기화
  React.useEffect(() => {
    setSearchTerm(query);
  }, [query]);

  // 페이지 이동 함수 (타겟 경로를 문자열로 받음)
  const handleNavigate = React.useCallback((target: string) => {
    // 검색어 초기화 로직 (다른 페이지로 이동 시)
    if (target !== '/search') {
      setSearchTerm('');
      setQuery('');
    }
    navigate(target);
  }, [navigate, setQuery]);


  const handleLogout = React.useCallback(() => {
    console.log('로그아웃 로직 실행');
    logout();
    setSearchTerm('');
    setQuery('');
    handleNavigate('/login');
  }, [handleNavigate, setQuery, logout]);

  const handleClickNoti = () => {
    markAllAsRead();
    handleNavigate("/notification");
  }


  // 검색어 입력하고 제출하면 검색 결과 페이지로 이동, 쿼리 파라미터로 검색어 전달.
  const handleSearchSubmit = React.useCallback(() => {
    if (searchTerm.trim()) {
      setQuery(searchTerm.trim());
      navigate(`/search?query=${searchTerm.trim()}`);
    }
  }, [searchTerm, navigate, setQuery]);


  const handleSearchChange = (term: string) => {
    setSearchTerm(term);
  };


  return (
    <div
      className={`sticky top-0 z-40 self-stretch h-[70px] min-w-96 px-6 flex items-center justify-center bg-my-white border-b border-my-border`}
    >
      <div className="flex w-full max-w-[1392px] justify-between">
        {/* 1. 왼쪽: 로고 (메인 페이지 이동) */}
        <button
          // 직접 onNavigate 함수를 호출하여 메인 페이지로 이동 로직 실행
          onClick={() => handleNavigate('/mypage')}
          className="focus:outline-none transition-opacity hover:opacity-80"
          aria-label="메인 페이지로 이동"
        >
          {/* SVG에 크기 클래스 명시 */}
          <Logo className="w-24 h-12" />
        </button>

        {/* 2. 중앙: 데스크톱 검색 입력 필드 */}
        <CenterSearchInput searchTerm={searchTerm} onSearchChange={handleSearchChange} onSearchSubmit={handleSearchSubmit} />

        {/* 2. 오른쪽 버튼 그룹 */}
        <div className="inline-flex justify-center items-center gap-4">
          {/* 💡 인증 상태에 따른 버튼 그룹 분기 */}
          {isAuthenticated ? (
            <>
              {/* 알림 버튼: 상위 컴포넌트의 토글 함수 호출 */}
              <button
                onClick={handleClickNoti }
                className="relative p-1 text-my-black focus:outline-none transition-opacity hover:opacity-70"
                aria-label="알림 확인"
              >
                {/* 아이콘 크기 명시 및 텍스트 색상을 따르도록 설정 */}
                <NotificationIcon className="w-6 h-6 fill-current" />
                {/* ⭐ 빨간 점 또는 숫자 뱃지 */}
                {hasUnread && (
                  <>
                    {/* 옵션 1: 단순 빨간 점 */}
                    <span className="absolute top-0 right-0 block h-2 w-2 rounded-full ring-2 ring-white bg-red-500" />

                    {/* 옵션 2: 숫자 표시 (선택) */}
                    {/* {unreadCount > 0 && (
                      <span className="absolute -top-1 -right-1 flex items-center justify-center min-w-[18px] h-[18px] px-1 text-xs font-bold text-white bg-red-500 rounded-full ring-2 ring-white">
                        {unreadCount > 99 ? '99+' : unreadCount}
                      </span>
                    )} */}
                  </>
                )}
              </button>

              {/* 마이 페이지 버튼 */}
              <button
                onClick={() => handleNavigate('/mypage')}
                className="p-1 text-my-black focus:outline-none transition-opacity hover:opacity-70"
                aria-label="마이 페이지"
              >
                <MyPageIcon className="w-6 h-6 fill-current" />
              </button>

              {/* 로그아웃 버튼 */}
              <button
                className="p-1 text-my-black focus:outline-none transition-opacity hover:opacity-70"
                aria-label="로그아웃"
                onClick={handleLogout}
              >
                <LogoutIcon className="w-6 h-6 fill-current" />
              </button>
            </>
          ) : (
            // === 미인증 상태: 로그인 버튼 ===
            <FilledButton
              onClick={() => handleNavigate('/login')}
              label="로그인"
              size="text-sm px-2 py-1"
              aria-label="로그인 페이지로 이동"
            />
          )}
        </div>
      </div>
      {/* 검색창 */}
    </div>
  );
};

export default MainHeader;
