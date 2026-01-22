import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import WorkSpacePage from '../workspace/WorkSpacePage';
import { useAuthStore } from '../../stores/authStore';

const PublicVersionViewerPage: React.FC = () => {
  const { projectKey, versionKey } = useParams<{ projectKey: string; versionKey: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const [isChecking, setIsChecking] = useState(true);

  useEffect(() => {
    const checkAccess = async () => {
      if (!projectKey || !versionKey) {
        // projectKey 또는 versionKey가 없으면 404로 이동
        navigate('/404', { replace: true });
        return;
      }

      // 로그인 체크
      // if (!currentUser) {
      //   // 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
      //   navigate('/login', { replace: true });
      //   return;
      // }

      // TODO: API 호출로 버전 정보 및 프로젝트 권한 확인
      // const response = await fetch(`/api/v1/search/projects/${projectKey}/versions/${versionKey}`);
      // const { hasEditAccess } = response.data;

      // 임시: 로그인한 사용자가 프로젝트 멤버인지 확인
      // 실제로는 API에서 isMember 정보를 받아와야 함
      const hasEditAccess = false; // TODO: 실제 API 응답에서 확인

      if (hasEditAccess && currentUser) {
        // 로그인한 프로젝트 멤버면 편집 모드로 리다이렉트
        navigate(`/project/${projectKey}/workspace`, { replace: true });
        return;
      }

      setIsChecking(false);
    };

    checkAccess();
  }, [projectKey, versionKey, currentUser, navigate]);

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

export default PublicVersionViewerPage;
