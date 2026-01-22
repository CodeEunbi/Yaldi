import React from 'react';
import { theme } from '../../styles/theme';

interface LinedButtonProps {
  label: string;
  onClick?: () => void;
  onClickEvent?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  disabled?: boolean;
  size?: string;
  type?: 'button' | 'submit' | 'reset';
}

const LinedButton = ({
  label,
  onClick,
  onClickEvent,
  disabled = false,
  size = '',
  type = 'button',
  ...rest
}: LinedButtonProps) => {
  return (
    <button
      onClick={onClick || onClickEvent}
      disabled={disabled}
      type={type}
      className={`
        ${size || 'px-[20px] py-[10px]'}
        bg-transparent text-${theme.myBlue} rounded-[10px] border border-${
        theme.myBlue
      }
        font-semibold
        hover:bg-${theme.myWhite} hover:text-${theme.myBlue}
        focus:outline-none
        disabled:opacity-50 disabled:hover:bg-transparent disabled:hover:text-${
          theme.myWhite
        } disabled:cursor-not-allowed
        transition-colors
        font-pretendard
      `}
      {...rest}
    >
      {label}
    </button>
  );
};

export default LinedButton;
