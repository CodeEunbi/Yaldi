import React from "react";
// import React, { useState } from 'react';
// import { useNavigate } from 'react-router-dom';
// import MainHeader from '../../components/common/MainHeader';

const LandingPage: React.FC = () => {
  // const navigate = useNavigate();
  // const [searchTerm, setSearchTerm] = useState('');
  // const [hasUnreadNotifications, setHasUnreadNotifications] = useState(false);

  // const handleNavigate = (target: 'main' | 'my') => {
  //   console.log(`[네비게이션] ${target} 페이지로 이동합니다.`);
  // };

  // const handleToggleNotifications = () => {
  //   setHasUnreadNotifications(false);
  // };

  // const handleLogout = () => {
  //   console.log('로그아웃');
  // };

  // const handleSearchChange = (term: string) => {
  //   setSearchTerm(term);
  // };

  // const handleSearchSubmit = (term: string) => {
  //   console.log(term, '검색하는 로직.');
  // };


  return (
    <div className="w-full min-h-screen bg-my-white flex flex-col">
      {/* <MainHeader
        onNavigate={handleNavigate}
        onToggleNotifications={handleToggleNotifications}
        onLogout={handleLogout}
        hasUnreadNotifications={hasUnreadNotifications}
        searchTerm={searchTerm}
        onSearchChange={handleSearchChange}
        onSearchSubmit={handleSearchSubmit}
        isAuthenticated={false}
      /> */}
      <div className="flex-1 flex items-center justify-center">
        <p className="text-xl">랜딩페이지가 도리 페이지</p>
      </div>
    </div>
  );
};

export default LandingPage;
