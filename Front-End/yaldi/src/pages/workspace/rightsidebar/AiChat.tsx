import React, { useState } from 'react';
import AiChatIcon from '../../../assets/icons/ai_chat_icon.svg?react';
import ChatBox from '../../../components/common/ChatBox';
import { theme } from '../../../styles/theme';
import SendIcon from '../../../assets/icons/send-icon.svg?react';
import UnSendIcon from '../../../assets/icons/unsend-icon.svg?react';

interface AiChatProps {
  isOpen: boolean;
  onToggle?: () => void;
  onClose: () => void;
  dimmed?: boolean;
}

type ChatMessage = {
  id: number;
  role: 'ai' | 'user';
  text: string;
  time: string;
};

const initialMessages: ChatMessage[] = [];

const AiChat: React.FC<AiChatProps> = ({
  isOpen,
  onToggle,
  onClose,
  dimmed = false,
}) => {
  const [message, setMessage] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);

  const handleClick = () => {
    onToggle?.();
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!message.trim()) {
      return;
    }

    const newMessage: ChatMessage = {
      id: Date.now(),
      role: 'user',
      text: message.trim(),
      time: new Date().toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
      }),
    };

    setMessages((prev) => [...prev, newMessage]);
    setMessage('');
  };

  return (
    <>
      <button
        onClick={handleClick}
        className="w-8 h-8 flex items-center justify-center mb-2 hover:bg-ai-chat rounded transition-colors"
        aria-label="AI 채팅"
      >
        <AiChatIcon className="w-5 h-5 text-my-black" />
      </button>

      <ChatBox
        isOpen={isOpen}
        onClose={onClose}
        title="AI 어시스턴트"
        dimmed={dimmed}
        footer={
          <form className="flex items-center gap-2" onSubmit={handleSubmit}>
            <label htmlFor="ai-chat-input" className="sr-only">
              메시지 입력
            </label>
            <input
              id="ai-chat-input"
              type="text"
              placeholder="메시지를 입력하세요"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              className="flex-1 rounded-full border border-my-border bg-my-white px-4 py-2 text-sm font-pretendard text-my-black focus:outline-none focus:ring-2 focus:ring-blue-200"
            />
            <button
              type="submit"
              disabled={!message.trim()}
              className="w-9 h-9 rounded-full flex items-center justify-center transition-colors bg-blue-500 text-white disabled:bg-transparent disabled:text-my-gray-400"
              aria-label="전송"
            >
              {message.trim() ? (
                <SendIcon className="w-5 h-5 text-my-white" />
              ) : (
                <UnSendIcon className="w-5 h-5 text-my-gray-400" />
              )}
            </button>
          </form>
        }
      >
        <div className="flex flex-col gap-3 pb-2">
          {messages.length === 0 ? (
            <div className="flex flex-1 flex-col items-center justify-center text-sm text-my-gray-500 font-pretendard gap-1 py-6">
              <span>아직 대화가 없습니다.</span>
              <span>아래 입력창에 메시지를 입력해보세요.</span>
            </div>
          ) : (
            messages.map((item) => {
              const isAi = item.role === 'ai';
              const bubbleClasses = isAi
                ? `self-start max-w-[80%] rounded-2xl rounded-bl-xl bg-${theme.myLightBlue} text-my-black px-3 py-2 shadow-sm`
                : `self-end max-w-[80%] rounded-2xl rounded-tr-xl bg-${theme.aiChat} text-my-black px-3 py-2 shadow-sm`;

              return (
                <div key={item.id} className={bubbleClasses}>
                  <p
                    className={`text-xs text-my-gray-500 font-pretendard mb-1 ${
                      isAi ? '' : 'text-right'
                    }`}
                  >
                    {item.time}
                  </p>
                  <p className="text-sm font-pretendard leading-relaxed">
                    {item.text}
                  </p>
                </div>
              );
            })
          )}
        </div>
      </ChatBox>
    </>
  );
};

export default AiChat;
