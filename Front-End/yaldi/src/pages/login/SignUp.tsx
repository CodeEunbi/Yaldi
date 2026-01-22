import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginVideo from '../../assets/video/login-video.mp4';
import { theme } from '../../styles/theme';
import ModalSmall from '../../components/common/ModalSmall';
import { apiController } from '../../apis/apiController';
import { useAuthStore } from '../../stores/authStore';
import { AxiosError } from 'axios';
import Swal from 'sweetalert2';

const SignUp: React.FC = () => {

  const currentUser = useAuthStore((state) => state.currentUser);
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

  const navigate = useNavigate();
  const [nicknameInput, setNicknameInput] = useState('');
  const [isNicknameDuplicate, setIsNicknameDuplicate] = useState(false);
  const [isNicknameTooLong, setIsNicknameTooLong] = useState(false);
  const [isNicknameEmptyModalOpen, setIsNicknameEmptyModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);


  // 길이 체크 - 10자 초과 시 입력 제한
  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;

    if (value.length > 10) {
      setIsNicknameTooLong(true);
      setIsNicknameDuplicate(false);
      return; // 10자 초과 입력 방지
    }

    setNicknameInput(value);
    setIsNicknameTooLong(false);
    setIsNicknameDuplicate(false);
  };


  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (isSubmitting) return;  // 중복 제출 방지


    // 닉네임이 비어있는지 확인
    if (!nicknameInput.trim()) {
      setIsNicknameEmptyModalOpen(true);
      return;
    }

    if (isNicknameTooLong) {
      return;
    }

    setIsSubmitting(true);  // 로딩 시작

    try {
      // 닉네임 수정 api 호출.
      await apiController({
        url: '/api/v1/users/me/nickname',
        method: 'patch',
        data: {
          nickname: nicknameInput.trim(),
        }
      })

      console.log('회원가입:', nicknameInput, currentUser?.email);

      // 업데이트된 유저 정보 다시 가져오기
      const userResponse = await apiController({
        url: '/api/v1/users/me',
        method: 'get',
      });

      // zustand store 전체 업데이트
      useAuthStore.setState({
        currentUser: userResponse.data.result
      });

      // 회원가입 성공 시 마이페이지로 이동
      navigate('/mypage');
    } catch (error: any) {
      console.error('회원가입 실패:', error);
      // 409면 닉네임 중복.
      if (error instanceof AxiosError && error.status === 409) {
        setIsNicknameDuplicate(true);
      }
      Swal.fire({
        icon: 'error',
        text: error.data.result,
        confirmButtonColor: '#1e50af',
      })
    } finally {
      setIsSubmitting(false);
    }
  };


  useEffect(() => {
    if (!isLoggedIn) {
      console.log("이 페이지는 회원 가입 중인 사용자만 접근 가능합니다.");
      navigate("/login", { replace: true });
    }
  }, [isLoggedIn]);


  return (
    <>
      {/* SignUp 페이지 전용 블러 강화 스타일 */}
      {(isNicknameEmptyModalOpen) && (
        <style>{`
          div.fixed.inset-0.z-50.flex.items-center.justify-center {
            backdrop-filter: blur(12px) !important;
            -webkit-backdrop-filter: blur(12px) !important;
          }
        `}</style>
      )}
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

        {/* 오른쪽 영역 - 회원가입 폼 */}
        <div className="flex-1 flex items-center justify-center bg-my-white">
          <div className="w-full max-w-md px-8">
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* 닉네임 입력 필드 */}
              <div className="space-y-2">
                <label
                  htmlFor="nickname"
                  className="block text-sm font-semibold text-my-black"
                >
                  닉네임
                </label>
                <input
                  id="nickname"
                  type="text"
                  value={nicknameInput}
                  onChange={handleNicknameChange}
                  placeholder="닉네임을 입력해주세요."
                  className={`w-full px-4 py-3 border-2 rounded-lg focus:outline-none focus:ring-1 transition-all duration-200 text-my-black placeholder:text-my-border ${isNicknameDuplicate || isNicknameTooLong
                    ? 'border-blue focus:border-blue focus:ring-blue'
                    : 'border-my-border focus:border-blue focus:ring-blue'
                    }`}
                />
                {isNicknameDuplicate && (
                  <div className="text-sm text-my-black">
                    <span className="text-blue">*</span> 중복된 닉네임입니다.
                  </div>
                )}
                {isNicknameTooLong && (
                  <div className="text-sm text-my-black">
                    <span className="text-blue">*</span> 최대 10글자입니다.
                  </div>
                )}
              </div>

              {/* 이메일 입력 필드 */}
              <div className="space-y-2">
                <label
                  htmlFor="email"
                  className="block text-sm font-semibold text-my-black"
                >
                  이메일
                </label>
                <input
                  id="email"
                  type="email"
                  value={currentUser?.email || ''}
                  disabled
                  readOnly
                  className="w-full px-4 py-3 border border-my-border rounded-lg bg-ai-chat text-my-border cursor-not-allowed"
                />
              </div>

              {/* 확인 버튼 */}
              <button
                type="submit"
                className="w-full py-3 bg-blue text-my-white rounded-lg font-semibold hover:bg-opacity-90 transition-all duration-200 shadow-sm hover:shadow-md"
                disabled={isSubmitting}
              >
                {isSubmitting ? '처리 중...' : '확인'}
              </button>
            </form>
          </div>
        </div>

        {/* 닉네임 미입력 모달 */}
        <ModalSmall
          isOpen={isNicknameEmptyModalOpen}
          onClose={() => setIsNicknameEmptyModalOpen(false)}
          title="입력 오류"
        >
          <div className="flex flex-col items-center gap-6 py-4">
            <p className="text-my-black text-center">
              닉네임을 입력해주세요.
            </p>
            <button
              onClick={() => setIsNicknameEmptyModalOpen(false)}
              className="px-8 py-3 bg-blue text-my-white rounded-lg font-semibold hover:bg-opacity-90 transition-all duration-200"
            >
              확인
            </button>
          </div>
        </ModalSmall>
      </div>
    </>
  );
};

export default SignUp;
