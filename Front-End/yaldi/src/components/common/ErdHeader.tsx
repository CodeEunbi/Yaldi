import { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import YaldiLogo from '../../assets/images/yaldi-logo.svg?react';
import EditIcon from '../../assets/icons/edit_icon.svg?react';
import CheckIcon from '../../assets/icons/check_icon.svg?react';
import NotificationIcon from '../../assets/icons/notification_icon.svg?react';
import MyPageIcon from '../../assets/icons/my_page_icon.svg?react';
import LogoutIcon from '../../assets/icons/logout_icon.svg?react';
import { useAuthStore } from '../../stores/authStore';
import { theme } from '../../styles/theme';
import FilledButton from './FilledButton';
import LinedButton from './LinedButton';
import InputBox from './InputBox';
import { useNotificationStore } from '../../stores/notificationStore';


// 모든 표준 HTML div 속성을 상속받도록 정의
interface ErdHeaderProps extends React.ComponentPropsWithoutRef<'div'> {
  onModeChange?: (mode: 'ver' | 'erd' | 'datamodel') => void;
  projectKey?: string;
}

const ErdHeader: React.FC<ErdHeaderProps> = ({
  onModeChange,
  projectKey: projectKeyProp,
  className = '',
  ...rest
}) => {
  const {
    currentMode,
    projectName,
    collaborators,
    setCurrentMode,
    setProjectName,
  } = useAuthStore();
  const [isEditingProjectName, setIsEditingProjectName] = useState(false);
  const [editedProjectName, setEditedProjectName] = useState(projectName);
  const navigate = useNavigate();
  const params = useParams<{ projectKey?: string }>();
  const location = useLocation();
  const projectKey = useMemo(
    () => projectKeyProp ?? params.projectKey ?? '1',
    [projectKeyProp, params.projectKey],
  );

  // ⭐ 알림 상태 구독
  const hasUnread = useNotificationStore((state) => state.hasUnread);
  const unreadCount = useNotificationStore((state) => state.unreadCount);

  // projectName이 변경될 때 editedProjectName 동기화
  useEffect(() => {
    setEditedProjectName(projectName);
  }, [projectName]);

  useEffect(() => {
    const path = location.pathname;
    let inferredMode: 'ver' | 'erd' | 'datamodel' = 'erd';

    if (path.includes('/data-model')) {
      inferredMode = 'datamodel';
    } else if (path.includes('/version')) {
      inferredMode = 'ver';
    } else if (path.includes('/workspace')) {
      inferredMode = 'erd';
    }

    if (currentMode !== inferredMode) {
      setCurrentMode(inferredMode);
      onModeChange?.(inferredMode);
    }
  }, [location.pathname, currentMode, setCurrentMode, onModeChange]);

  const handleNavigate = useCallback(
    (target: string) => {
      navigate(target);
    },
    [navigate],
  );

  const handleLogout = useCallback(() => {
    handleNavigate('/login');
    console.log('로그아웃 로직 실행');
  }, [handleNavigate]);


  const handleModeChange = (mode: 'ver' | 'erd' | 'datamodel') => {
    setCurrentMode(mode);
    onModeChange?.(mode);

    if (mode === 'datamodel') {
      navigate(`/project/${projectKey}/data-model`);
      return;
    }

    if (mode === 'ver') {
      navigate(`/project/${projectKey}/version`);
      return;
    }

    navigate(`/project/${projectKey}/workspace`);
  };

  const handleProjectNameEdit = () => {
    if (isEditingProjectName) {
      setProjectName(editedProjectName);
    }
    setIsEditingProjectName(!isEditingProjectName);
  };

  const handleProjectNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEditedProjectName(e.target.value);
  };

  const handleProjectNameKeyDown = (
    e: React.KeyboardEvent<HTMLInputElement>,
  ) => {
    if (e.key === 'Enter') {
      handleProjectNameEdit();
    } else if (e.key === 'Escape') {
      setEditedProjectName(projectName);
      setIsEditingProjectName(false);
    }
  };

  // 사용자 이름의 첫 글자 추출
  const getInitial = (name: string) => {
    return name.charAt(0).toUpperCase();
  };

  // 아바타 배경색 클래스 매핑
  const getAvatarBgClass = (avatarColor?: string) => {
    const colorMap: Record<string, string> = {
      user1: `bg-${theme.myBlue}`,
      user2: `bg-${theme.user2}`,
      user3: `bg-${theme.user3}`,
      user4: `bg-${theme.user4}`,
      user5: `bg-${theme.user5}`,
      user6: `bg-${theme.user6}`,
      user7: `bg-${theme.user7}`,
      user8: `bg-${theme.user8}`,
    };
    return avatarColor && colorMap[avatarColor]
      ? colorMap[avatarColor]
      : `bg-${theme.user2}`;
  };

  return (
    // Tailwind 클래스 정리: sticky를 사용하여 헤더 고정 가능
    <>
      <div
        className={`sticky top-0 z-40 self-stretch h-[70px] min-w-96 px-6 flex items-center justify-center bg-my-white border-b border-my-border ${className}`}
        {...rest}
      >
        <div className="flex w-full max-w-[1392px] justify-between">
          {/* 왼쪽: 로고, 프로젝트 제목, 편집 버튼, 네비게이션 버튼 */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => handleNavigate('/mypage')}
              className="flex items-center focus:outline-none transition-opacity hover:opacity-70"
              aria-label="마이 페이지 이동"
            >
              <YaldiLogo className="w-24 h-12" />
            </button>
            <div className="flex items-center gap-2">
              {isEditingProjectName ? (
                <InputBox
                  value={editedProjectName}
                  onChange={handleProjectNameChange}
                  onKeyDown={handleProjectNameKeyDown}
                  onBlur={handleProjectNameEdit}
                  size="px-2 py-1 text-base"
                  className="font-medium"
                  autoFocus
                />
              ) : (
                <span className="text-my-black text-base font-medium font-pretendard">
                  {projectName}
                </span>
              )}
            </div>
            <button
              onClick={handleProjectNameEdit}
              className={`p-1 focus:outline-none transition-opacity hover:opacity-70 ${isEditingProjectName ? `text-${theme.myBlue}` : 'text-gray-400'
                }`}
            >
              {isEditingProjectName ? (
                <CheckIcon className="w-6 h-6" />
              ) : (
                <EditIcon className="w-6 h-6" />
              )}
            </button>
            {/* 네비게이션 버튼 */}
            <div className="flex items-center gap-2 ml-2">
              {currentMode === 'ver' ? (
                <FilledButton
                  label="Ver."
                  onClick={() => handleModeChange('ver')}
                  size="px-4 py-2 text-sm"
                />
              ) : (
                <LinedButton
                  label="Ver."
                  onClick={() => handleModeChange('ver')}
                  size="px-4 py-2 text-sm"
                />
              )}
              {currentMode === 'erd' ? (
                <FilledButton
                  label="ERD"
                  onClick={() => handleModeChange('erd')}
                  size="px-4 py-2 text-sm"
                />
              ) : (
                <LinedButton
                  label="ERD"
                  onClick={() => handleModeChange('erd')}
                  size="px-4 py-2 text-sm"
                />
              )}
              {currentMode === 'datamodel' ? (
                <FilledButton
                  label="Data Model"
                  onClick={() => handleModeChange('datamodel')}
                  size="px-4 py-2 text-sm"
                />
              ) : (
                <LinedButton
                  label="Data Model"
                  onClick={() => handleModeChange('datamodel')}
                  size="px-4 py-2 text-sm"
                />
              )}
            </div>
          </div>

          {/* 오른쪽: 사용자 아바타 및 아이콘 */}
          <div className="flex items-center gap-4">
            {/* 사용자 아바타 */}
            <div className="flex items-center">
              {collaborators.map((collaborator, index) => (
                <div
                  key={collaborator.userKey}
                  className={`w-10 h-10 rounded-full ${getAvatarBgClass(
                    collaborator.avatarColor,
                  )} flex items-center justify-center text-white text-base font-medium border-2 border-white relative ${index > 0 ? '-ml-2' : ''
                    }`}
                  style={{ zIndex: 10 + index }}
                >
                  {getInitial(collaborator.nickname)}
                </div>
              ))}
            </div>

            {/* 2. 오른쪽 버튼 그룹 */}
            <div className="inline-flex justify-center items-center gap-4">
              {/* 알림 버튼 */}
              <button
                onClick={() => navigate("/notification")}
                className="relative p-1 text-my-black focus:outline-none transition-opacity hover:opacity-70"
                aria-label="알림 확인"
              >
                {/* 아이콘 크기 명시 및 텍스트 색상을 따르도록 설정 */}
                <NotificationIcon className="w-6 h-6 fill-current" />
                {/* 💡 알림 뱃지: 읽지 않은 알림이 있을 때만 표시 */}
                {/* ⭐ 빨간 점 또는 숫자 뱃지 */}
                {/* ⭐ 빨간 점 또는 숫자 뱃지 */}
                {hasUnread && (
                  <>
                    {/* 옵션 1: 단순 빨간 점 */}
                    {/* <span className="absolute top-0 right-0 block h-2 w-2 rounded-full ring-2 ring-white bg-red-500" /> */}

                    {/* 옵션 2: 숫자 표시 (선택) */}
                    {unreadCount > 0 && (
                      <span className="absolute -top-1 -right-1 flex items-center justify-center min-w-[18px] h-[18px] px-1 text-xs font-bold text-white bg-red-500 rounded-full ring-2 ring-white">
                        {unreadCount > 99 ? '99+' : unreadCount}
                      </span>
                    )}
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
                onClick={handleLogout}
                className="p-1 text-my-black focus:outline-none transition-opacity hover:opacity-70"
                aria-label="로그아웃"
              >
                <LogoutIcon className="w-6 h-6 fill-current" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default ErdHeader;
