import React from 'react';
import GoogleIcon from '../../assets/icons/google-icon.svg?react';
import GitHubIcon from '../../assets/icons/github-icon.svg?react';
import SSAFYIcon from '../../assets/icons/ssafy-icon.svg?react';
import LoginVideo from '../../assets/video/login-video.mp4';
import { theme } from '../../styles/theme';

const baseURL = import.meta.env.VITE_API_BASE_URL;

const Login: React.FC = () => {

  // ✅ 수정 - 공통 함수로 통합
  const handleOAuthLogin = (provider: 'google' | 'github' | 'ssafy') => {
    console.log(`${provider} 로그인`);
    window.location.replace(`${baseURL}/oauth2/authorization/${provider}`);
  };


  return (
    <div className="flex h-screen w-full bg-loginleft">
      {/* 왼쪽 영역 - 영상 */}
      <div
        className="flex-1 flex items-center justify-center overflow-hidden"
        style={{ backgroundColor: theme.loginLeft }}
      >
        <video
          src={LoginVideo}
          autoPlay
          loop
          muted
          playsInline
          className="w-full h-full object-contain"
        />
      </div>

      {/* 오른쪽 영역 - 로그인 버튼들 */}
      <div className="flex-1 flex items-center justify-center bg-my-white">
        <div className="w-full max-w-md px-8 space-y-6">
          <h1 className="text-3xl font-bold text-my-black mb-8 text-center">
            Yaldi에 오신 것을 환영합니다
          </h1>

          {/* Google 로그인 버튼 */}
          <button
            onClick={() => handleOAuthLogin('google')}
            className="w-full flex items-center justify-center gap-4 px-6 py-4 bg-my-white border-2 border-my-border rounded-xl hover:border-blue hover:shadow-md transition-all duration-200 font-semibold text-my-black"
          >
            <GoogleIcon className="w-6 h-6" />
            <span>Log in with Google</span>
          </button>

          {/* GitHub 로그인 버튼 */}
          <button
            onClick={() => handleOAuthLogin('github')}
            className="w-full flex items-center justify-center gap-4 px-6 py-4 bg-my-white border-2 border-my-border rounded-xl hover:border-blue hover:shadow-md transition-all duration-200 font-semibold text-my-black"
          >
            <GitHubIcon className="w-6 h-6" />
            <span>Log in with GitHub</span>
          </button>

          {/* SSAFY 로그인 버튼 */}
          <button
            disabled={true}
            onClick={() => handleOAuthLogin('ssafy')}
            className="w-full flex items-center justify-center gap-4 px-6 py-4 bg-my-white border-2 border-my-border rounded-xl hover:border-blue hover:shadow-md transition-all duration-200 font-semibold text-my-black"
          >
            <SSAFYIcon className="w-6 h-6" />
            <span>Log in with SSAFY</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Login;
