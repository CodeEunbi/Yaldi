// utils/myNavigation.ts

import type { NavigateFunction } from 'react-router-dom';

class MyNavigation {
  private static navigate: NavigateFunction | null = null;

  // 네비게이션 함수 설정
  public static setNavigate(navigateFunction: NavigateFunction): void {
    this.navigate = navigateFunction;
  }

  // 로그인 페이지로 이동
  public static goToLogin(): void {
    if (this.navigate) {
      this.navigate('/login');
    } else {
      window.location.href = '/login';
    }
  }


  // 사용자 지정 경로로 이동
  public static goTo(path: string): void {
    if (this.navigate) {
      this.navigate(path);
    } else {
      window.location.href = path;
    }
  }
}

export default MyNavigation;
