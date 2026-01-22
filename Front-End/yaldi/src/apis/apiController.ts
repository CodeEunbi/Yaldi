import axios from "axios";
import type {
  AxiosInstance,
  AxiosResponse,
  AxiosError,
} from 'axios';
import navigationService from '../utils/MyNavigation';
import Swal from "sweetalert2";
import { useAuthStore } from "../stores/authStore";


const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';


// Axios 인스턴스 생성
export const apiController: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  timeout: 5000,
  headers: {
    "Content-Type": "application/json",
  },
});


// 응답 인터셉터
apiController.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    // 2xx 성공
    return response;
  },
  async (error: AxiosError): Promise<AxiosResponse | never> => {
    // 에러 응답이 존재하는지 확인
    if (!error.response) {
      return Promise.reject(error);
    }

    const { status } = error.response;
    console.log('Error response:', error.response);

    if (status === 401) {
      // 401 토큰 만료

      Swal.fire({
        icon: 'warning',
        text: '로그인 정보가 만료되었습니다. 다시 로그인해주세요.',
        confirmButtonColor: '#1e50af'
      }).then(() => {
        useAuthStore.getState().logout();
        navigationService.goToLogin();
      })
    }
    return Promise.reject(error.response);
  },
);

