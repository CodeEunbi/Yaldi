import React, { useState, useEffect, useMemo } from 'react';
import ShareIcon from '../../../assets/icons/share_icon.svg?react';
import CopyIcon from '../../../assets/icons/copy_icon.svg?react';
import BuildSuccessIcon from '../../../assets/icons/build_success_icon.svg?react';
import ModalSmall from '../../../components/common/ModalSmall';
import InputBox from '../../../components/common/InputBox';

interface ShareProps {
  shareLink?: string;
  expiresAt?: string | Date; // 유효기간 (ISO 문자열 또는 Date 객체)
  onModalChange?: (open: boolean) => void;
}

const Share: React.FC<ShareProps> = ({
  shareLink = 'https://yaldi.co.kr/project/1234567890',
  expiresAt: expiresAtProp,
  onModalChange,
}) => {
  // expiresAt을 한 번만 계산하도록 useMemo 사용
  const expiresAt = useMemo(() => {
    if (expiresAtProp) {
      return expiresAtProp;
    }
    // 테스트용: 3일 후 만료 (한 번만 계산)
    return new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString();
  }, [expiresAtProp]);

  // 만료일 문자열도 한 번만 계산
  const expirationDateString = useMemo(() => {
    if (!expiresAt) return '';
    return new Date(expiresAt).toLocaleString('ko-KR');
  }, [expiresAt]);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showCopyToast, setShowCopyToast] = useState(false);
  const [timeRemaining, setTimeRemaining] = useState<{
    days: number;
    hours: number;
    minutes: number;
    seconds: number;
    isExpired: boolean;
  } | null>(null);

  const handleOpen = () => {
    setIsModalOpen(true);
    onModalChange?.(true);
  };

  const handleClose = () => {
    setIsModalOpen(false);
    onModalChange?.(false);
  };

  useEffect(() => {
    if (!showCopyToast) return;

    const timer = window.setTimeout(() => {
      setShowCopyToast(false);
    }, 2000);

    return () => window.clearTimeout(timer);
  }, [showCopyToast]);

  // 유효기간 카운트다운 계산
  useEffect(() => {
    if (!expiresAt) {
      setTimeRemaining(null);
      return;
    }

    const calculateTimeRemaining = () => {
      const expirationDate = new Date(expiresAt);
      const now = new Date();
      const diff = expirationDate.getTime() - now.getTime();

      if (diff <= 0) {
        setTimeRemaining({
          days: 0,
          hours: 0,
          minutes: 0,
          seconds: 0,
          isExpired: true,
        });
        return;
      }

      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      setTimeRemaining({
        days,
        hours,
        minutes,
        seconds,
        isExpired: false,
      });
    };

    // 즉시 계산
    calculateTimeRemaining();

    // 1초마다 업데이트
    const interval = setInterval(calculateTimeRemaining, 1000);

    return () => clearInterval(interval);
  }, [expiresAt]);

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareLink);
      setShowCopyToast(true);
    } catch (error) {
      console.error('링크 복사 실패:', error);
    }
  };

  return (
    <>
      <button
        onClick={handleOpen}
        className="w-8 h-8 flex items-center justify-center mb-2 hover:bg-ai-chat rounded transition-colors"
        aria-label="공유"
      >
        <ShareIcon className="w-5 h-5 text-my-black" />
      </button>

      <ModalSmall
        isOpen={isModalOpen}
        onClose={handleClose}
        title="프로젝트 공유 링크"
      >
        <div className="flex flex-col gap-4 w-full">
          <p className="text-my-black text-base font-pretendard">
            아래 링크를 통해 뷰어를 초대하세요.
          </p>

          <div className="flex flex-col gap-2">
            <label className="text-my-black text-sm font-semibold font-pretendard">
              링크
            </label>
            <div className="flex gap-2">
              <InputBox
                type="text"
                value={shareLink}
                readOnly
                className="flex-1"
                size="px-[20px] py-[10px]"
              />
              <button
                onClick={handleCopyLink}
                className="px-2 py-2 border-2 border-my-border rounded-[10px] hover:bg-gray-50 active:bg-gray-100 active:scale-95 transition-all duration-150 flex items-center justify-center"
                aria-label="링크 복사"
              >
                <CopyIcon className="w-5 h-5 text-my-black" />
              </button>
            </div>
          </div>

          {expiresAt && (
            <div className="flex flex-col gap-2">
              <label className="text-my-black text-sm font-semibold font-pretendard">
                유효기간
              </label>
              <div className="px-4 py-3 bg-gray-50 rounded-[10px] border border-my-border">
                {timeRemaining ? (
                  timeRemaining.isExpired ? (
                    <p className="text-red-500 text-sm font-pretendard font-medium">
                      링크가 만료되었습니다.
                    </p>
                  ) : (
                    <div className="flex flex-col gap-1">
                      <p className="text-my-black text-sm font-pretendard">
                        {timeRemaining.days > 0 && (
                          <span className="font-semibold">{timeRemaining.days}일 </span>
                        )}
                        {timeRemaining.hours > 0 && (
                          <span className="font-semibold">{timeRemaining.hours}시간 </span>
                        )}
                        {timeRemaining.minutes > 0 && (
                          <span className="font-semibold">{timeRemaining.minutes}분 </span>
                        )}
                        <span className="font-semibold">{timeRemaining.seconds}초</span>
                        <span className="text-my-gray-500"> 남았습니다</span>
                      </p>
                      <p className="text-xs text-my-gray-500 font-pretendard">
                        만료일: {expirationDateString}
                      </p>
                    </div>
                  )
                ) : (
                  <p className="text-my-gray-500 text-sm font-pretendard">
                    계산 중...
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      </ModalSmall>

      {showCopyToast && (
        <div className="fixed bottom-6 left-1/2 z-50">
          <div className="transform -translate-x-1/2">
            <div className="bg-my-black text-my-white px-6 py-3 rounded-[10px] shadow-lg flex items-center gap-2 animate-fadeIn">
              <BuildSuccessIcon className="w-5 h-5" />
              <span className="font-pretendard text-sm font-medium">
                링크가 복사되었습니다.
              </span>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default Share;
