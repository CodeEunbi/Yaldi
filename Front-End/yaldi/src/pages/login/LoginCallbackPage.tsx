import React from "react";
import { apiController } from "../../apis/apiController";
import { useAuthStore } from "../../stores/authStore";
import { useNavigate } from "react-router-dom";


export const LoginRedirect: React.FC = () => {
  const [isLoading, setIsLoading] = React.useState<boolean>(true); // ✅ 수정 1: 초기값을 true로 변경
  const login = useAuthStore((state) => state.login);
  const navigate = useNavigate();
  let params = new URL(document.location.toString()).searchParams;

  // ✅ 수정 2: 함수명 오타 수정 (getUesr -> getUser)
  const getUser = async () => {
    const response = await apiController({
      url: '/api/v1/users/me',
      method: 'get',
    });
    login(response.data.result);
  };

  React.useEffect(() => {

    const handleAuthCallback = async () => {
      try {
        // 1. 사용자 정보 가져오기
        await getUser();

        // 2. URL 쿼리 파라미터에서 authType 추출
        const actionType = params.get("authType");

        console.log("인증 타입 확인:", actionType);

        // 3. authType에 따른 리디렉션 처리
        if (actionType === "LOGIN") {
          navigate("/mypage", { replace: true });
        } else if (actionType === "SIGNUP" || actionType === "REJOIN") {
          navigate("/signup", { replace: true });
        } else {
          console.warn("예상치 못한 actionType:", actionType);
          navigate("/", { replace: true });
        }
      } catch (err) {
        console.error("인증 처리 중 실패:", err);
        navigate("/login", { replace: true });
      } finally {
        setIsLoading(false);
      }
    };

    handleAuthCallback();
    // ✅ 수정 3: 의존성 배열 최적화 - 빈 배열로 변경하여 마운트 시 한 번만 실행
  }, []);


  return (
    <>
      {
        isLoading ?
          (
            <div className='flex justify-center' >
              <svg aria-hidden="true" className="w-8 h-8 text-gray-200 animate-spin dark:text-gray-600 fill-blue-600" viewBox="0 0 100 101" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M100 50.5908C100 78.2051 77.6142 100.591 50 100.591C22.3858 100.591 0 78.2051 0 50.5908C0 22.9766 22.3858 0.59082 50 0.59082C77.6142 0.59082 100 22.9766 100 50.5908ZM9.08144 50.5908C9.08144 73.1895 27.4013 91.5094 50 91.5094C72.5987 91.5094 90.9186 73.1895 90.9186 50.5908C90.9186 27.9921 72.5987 9.67226 50 9.67226C27.4013 9.67226 9.08144 27.9921 9.08144 50.5908Z" fill='#CFCFCF' />
                <path d="M93.9676 39.0409C96.393 38.4038 97.8624 35.9116 97.0079 33.5539C95.2932 28.8227 92.871 24.3692 89.8167 20.348C85.8452 15.1192 80.8826 10.7238 75.2124 7.41289C69.5422 4.10194 63.2754 1.94025 56.7698 1.05124C51.7666 0.367541 46.6976 0.446843 41.7345 1.27873C39.2613 1.69328 37.813 4.19778 38.4501 6.62326C39.0873 9.04874 41.5694 10.4717 44.0505 10.1071C47.8511 9.54855 51.7191 9.52689 55.5402 10.0491C60.8642 10.7766 65.9928 12.5457 70.6331 15.2552C75.2735 17.9648 79.3347 21.5619 82.5849 25.841C84.9175 28.9121 86.7997 32.2913 88.1811 35.8758C89.083 38.2158 91.5421 39.6781 93.9676 39.0409Z" fill='#1E50AF' />
              </svg>
              <span className="sr-only">Loading...</span>
            </div >) : (
            <></>
          )}
    </>

  );
};