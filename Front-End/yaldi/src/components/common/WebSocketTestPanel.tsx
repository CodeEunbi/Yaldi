/**
 * WebSocket 테스트 패널
 * 개발 중에만 사용하는 테스트용 컴포넌트
 */

import { useState } from 'react';

interface WebSocketTestPanelProps {
  isConnected: boolean;
  onConnect: () => void;
  onDisconnect: () => void;
  onSendTestMessage: () => void;
}

export const WebSocketTestPanel: React.FC<WebSocketTestPanelProps> = ({
  isConnected,
  onConnect,
  onDisconnect,
  onSendTestMessage,
}) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="fixed bottom-4 right-4 z-[10000]">
      {/* 토글 버튼 */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="rounded-full bg-blue-600 px-4 py-2 text-white shadow-lg hover:bg-blue-700"
      >
        {isOpen ? '닫기' : 'WS 테스트'}
      </button>

      {/* 테스트 패널 */}
      {isOpen && (
        <div className="absolute bottom-12 right-0 w-80 rounded-lg bg-white p-4 shadow-xl border border-gray-200">
          <h3 className="mb-3 font-bold text-lg">WebSocket 테스트</h3>

          {/* 연결 상태 */}
          <div className="mb-3">
            <div className="flex items-center gap-2">
              <div
                className={`h-3 w-3 rounded-full ${
                  isConnected ? 'bg-green-500' : 'bg-red-500'
                }`}
              />
              <span className="text-sm font-medium">
                {isConnected ? '연결됨' : '연결 안 됨'}
              </span>
            </div>
          </div>

          {/* 버튼들 */}
          <div className="flex flex-col gap-2">
            {!isConnected ? (
              <button
                onClick={onConnect}
                className="rounded bg-green-600 px-4 py-2 text-white hover:bg-green-700"
              >
                연결하기
              </button>
            ) : (
              <>
                <button
                  onClick={onDisconnect}
                  className="rounded bg-red-600 px-4 py-2 text-white hover:bg-red-700"
                >
                  연결 끊기
                </button>
                <button
                  onClick={onSendTestMessage}
                  className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
                >
                  테스트 메시지 전송
                </button>
              </>
            )}
          </div>

          {/* 안내 */}
          <div className="mt-3 rounded bg-gray-100 p-2 text-xs text-gray-600">
            <p className="font-semibold mb-1">테스트 방법:</p>
            <ol className="list-decimal list-inside space-y-1">
              <li>백엔드 서버 실행 확인</li>
              <li>"연결하기" 클릭</li>
              <li>콘솔에서 연결 로그 확인</li>
              <li>"테스트 메시지 전송" 클릭</li>
            </ol>
          </div>
        </div>
      )}
    </div>
  );
};
