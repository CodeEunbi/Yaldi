// hooks/useNotificationStream.ts
import { useEffect, useCallback } from "react";
import { useNotificationStore } from "../stores/notificationStore";
import { useAuthStore } from "../stores/authStore";

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

interface NotificationData {
  notificationKey: number;
  type: string;
  content: string;
  invitationKey?: number;
  createdAt: string;
  isRead: boolean;
}

const useNotificationStream = () => {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);
  const incrementUnread = useNotificationStore((state) => state.incrementUnread);
  const setLastNotificationId = useNotificationStore((state) => state.setLastNotificationId);

  const handleNewNotification = useCallback((data: NotificationData) => {
    console.log("새 알림:", data);
    
    // 읽지 않은 알림이면 카운트 증가
    if (!data.isRead) {
      incrementUnread();
      setLastNotificationId(data.notificationKey);
    }
    
    // 선택사항: 브라우저 알림 표시
    // if (Notification.permission === "granted") {
    //   new Notification("새 알림", {
    //     body: data.content,
    //     icon: "/yaldi-logo.svg",
    //   });
    // }
  }, [incrementUnread, setLastNotificationId]);

  useEffect(() => {
    // 로그인 안 되어 있으면 연결 안 함
    if (!isLoggedIn) {
      return;
    }

    let eventSource: EventSource | null = null;
    let reconnectTimeout: number | null = null; // ⭐ number로 변경

    const connect = () => {
      // 이전 재연결 타이머 제거
      if (reconnectTimeout !== null) {
        clearTimeout(reconnectTimeout);
        reconnectTimeout = null;
      }

      eventSource = new EventSource(`${BASE_URL}/api/v1/notifications/stream`, {
        withCredentials: true,
      });

      eventSource.addEventListener("notification", (event: MessageEvent) => {
        try {
          const data: NotificationData = JSON.parse(event.data);
          handleNewNotification(data);
        } catch (error) {
          console.error("알림 파싱 에러:", error);
        }
      });

      eventSource.addEventListener("open", () => {
        console.log("SSE 연결 성공");
      });

      eventSource.onerror = (error) => {
        console.warn("SSE 연결 오류:", error);
        eventSource?.close();
        
        // 로그인 상태일 때만 재연결 시도
        if (isLoggedIn) {
          console.log("3초 후 재연결 시도...");
          reconnectTimeout = window.setTimeout(connect, 3000); // ⭐ window.setTimeout으로 명시
        }
      };
    };

    // 브라우저 알림 권한 요청 (선택사항)
    if (Notification.permission === "default") {
      Notification.requestPermission();
    }

    connect();

    return () => {
      if (reconnectTimeout !== null) {
        clearTimeout(reconnectTimeout);
      }
      eventSource?.close();
    };
  }, [isLoggedIn, handleNewNotification]);
};

export default useNotificationStream;