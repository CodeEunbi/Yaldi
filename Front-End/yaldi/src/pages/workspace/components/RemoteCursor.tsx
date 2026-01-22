import React from 'react';

type RemoteCursorProps = {
  userName: string;
  userColor: string;
  xPosition: number;
  yPosition: number;
};

const RemoteCursor: React.FC<RemoteCursorProps> = ({
  userName,
  userColor,
  xPosition,
  yPosition,
}) => {
  return (
    <div
      className="pointer-events-none fixed z-[9999] transition-transform duration-100 ease-out"
      style={{
        left: `${xPosition}px`,
        top: `${yPosition}px`,
        transform: 'translate(-2px, -2px)',
      }}
    >
      {/* 커서 아이콘 (화살표) */}
      <svg
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M5.65376 3.42712C5.27994 2.84024 5.99355 2.12663 6.58044 2.50045L20.4752 10.6456C21.0452 11.0092 21.0452 11.8443 20.4752 12.208L14.058 16.0576L11.5609 21.6346C11.2868 22.251 10.408 22.251 10.1339 21.6346L5.65376 11.208V3.42712Z"
          fill={userColor}
          stroke="white"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
      </svg>

      {/* 사용자 이름 라벨 */}
      <div
        className="ml-5 mt-1 whitespace-nowrap rounded-md px-2 py-1 text-xs font-medium text-white shadow-md"
        style={{
          backgroundColor: userColor,
        }}
      >
        {userName}
      </div>
    </div>
  );
};

export default RemoteCursor;
