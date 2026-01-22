import React from "react";
import ErrorIcon from "../assets/icons/warning_icon.svg?react";
import { useAuthStore } from "../stores/authStore";
import FilledButton from "../components/common/FilledButton";
import { useNavigate } from "react-router-dom";

const NotFoundPage: React.FC = () => {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const navigate = useNavigate();

  return (
    <div className="w-full h-full flex flex-col gap-10 pt-36 justify-center items-center content-center text-my-black font-pretendard">
      <ErrorIcon className="h-36 w-36" />
      <h3>찾을 수 없는 페이지입니다.</h3>
      <FilledButton label={isLoggedIn ? "마이페이지로" : "메인으로"} onClick={() => isLoggedIn ? navigate("/mypage", {replace: true}) : navigate("/", {replace: true})} />
    </div>
  );
}


export default NotFoundPage;