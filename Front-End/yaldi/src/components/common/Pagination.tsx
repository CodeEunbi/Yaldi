import React from 'react';
import LeftIcon from '../../assets/icons/left_icon.svg?react';
import RightIcon from '../../assets/icons/right_icon.svg?react';
import { theme } from '../../styles/theme';

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  onPageChange,
  className = '',
}) => {
  // 페이지 번호 배열 생성
  const getPageNumbers = () => {
    const pages: (number | string)[] = [];
    const maxVisible = 5; // 최대 표시할 페이지 수

    if (totalPages <= maxVisible) {
      // 전체 페이지가 5개 이하면 모두 표시
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      // 시작 페이지
      if (currentPage <= 3) {
        for (let i = 1; i <= 4; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages);
      }
      // 끝 페이지
      else if (currentPage >= totalPages - 2) {
        pages.push(1);
        pages.push('...');
        for (let i = totalPages - 3; i <= totalPages; i++) {
          pages.push(i);
        }
      }
      // 중간 페이지
      else {
        pages.push(1);
        pages.push('...');
        for (let i = currentPage - 1; i <= currentPage + 1; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages);
      }
    }

    return pages;
  };

  const handlePrevious = () => {
    if (currentPage > 1) {
      onPageChange(currentPage - 1);
    }
  };

  const handleNext = () => {
    if (currentPage < totalPages) {
      onPageChange(currentPage + 1);
    }
  };

  const handlePageClick = (page: number | string) => {
    if (typeof page === 'number') {
      onPageChange(page);
    }
  };

  const pageNumbers = getPageNumbers();

  return (
    <div className={`flex items-center justify-center gap-2 ${className}`}>
      {/* 왼쪽 화살표 */}
      <button
        onClick={handlePrevious}
        disabled={currentPage === 1}
        className={`p-2 flex items-center justify-center ${
          currentPage === 1
            ? 'opacity-30 cursor-not-allowed'
            : 'hover:opacity-70 transition-opacity'
        }`}
        aria-label="이전 페이지"
      >
        <LeftIcon className="w-6 h-6 fill-current text-my-black" />
      </button>

      {/* 페이지 번호들 */}
      {pageNumbers.map((page, index) => {
        if (page === '...') {
          return (
            <span
              key={`ellipsis-${index}`}
              className="px-2 text-my-black font-semibold font-pretendard"
            >
              ...
            </span>
          );
        }

        const pageNum = page as number;
        const isActive = pageNum === currentPage;

        return (
          <button
            key={pageNum}
            onClick={() => handlePageClick(pageNum)}
            className={`w-10 h-10 rounded-full flex items-center justify-center font-semibold font-pretendard transition-colors ${
              isActive
                ? `bg-${theme.myBlue} text-${theme.myWhite}`
                : `bg-transparent text-my-black hover:bg-${theme.myLightBlue} hover:text-my-black`
            }`}
            aria-label={`${pageNum}페이지로 이동`}
            aria-current={isActive ? 'page' : undefined}
          >
            {pageNum}
          </button>
        );
      })}

      {/* 오른쪽 화살표 */}
      <button
        onClick={handleNext}
        disabled={currentPage === totalPages}
        className={`p-2 flex items-center justify-center ${
          currentPage === totalPages
            ? 'opacity-30 cursor-not-allowed'
            : 'hover:opacity-70 transition-opacity'
        }`}
        aria-label="다음 페이지"
      >
        <RightIcon className="w-6 h-6 fill-current text-my-black" />
      </button>
    </div>
  );
};

export default Pagination;
