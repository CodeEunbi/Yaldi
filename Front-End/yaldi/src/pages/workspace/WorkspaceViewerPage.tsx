import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import WorkSpacePage from './WorkSpacePage';
import { useAuthStore } from '../../stores/authStore';

const WorkspaceViewerPage: React.FC = () => {
  const { viewerLinkKey } = useParams<{ viewerLinkKey: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const [isChecking, setIsChecking] = useState(true);

  useEffect(() => {
    const checkAccess = async () => {
      if (!viewerLinkKey) {
        // viewerLinkKey가 없으면 404로 이동
        navigate('/404', { replace: true });
        return;
      }

      // TODO: API 호출로 뷰어 링크에 해당하는 프로젝트 정보 가져오기
      // const response = await fetch(`/api/viewer/${viewerLinkKey}`);
      // const { projectKey, hasEditAccess } = response.data;

      // 임시: 로그인한 사용자는 편집 권한이 있다고 가정
      const hasEditAccess = !!currentUser;

      if (hasEditAccess) {
        // 로그인한 프로젝트 멤버면 편집 모드로 리다이렉트
        // TODO: 실제 projectKey를 사용
        // navigate(`/project/${projectKey}/workspace`, { replace: true });
        console.log('프로젝트 멤버입니다. 편집 모드로 리다이렉트해야 합니다.');
      }

      setIsChecking(false);
    };

    checkAccess();
  }, [viewerLinkKey, currentUser, navigate]);

  // 권한 체크 중이면 로딩 표시
  if (isChecking) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-lg">로딩 중...</div>
      </div>
    );
  }

  // 뷰어 모드로 워크스페이스 표시
  return <WorkSpacePage mode="view" />;
};

export default WorkspaceViewerPage;
