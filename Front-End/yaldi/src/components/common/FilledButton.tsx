import React from 'react';
import { theme } from '../../styles/theme';

interface FilledButtonProps {
  label: string;
  onClick?: () => void;
  onClickEvent?: (e: React.MouseEvent<HTMLButtonElement>) => void;
  disabled?: boolean;
  size?: string;
  type?: 'button' | 'submit' | 'reset';
}

const FilledButton = ({
  label,
  onClick,
  onClickEvent,
  disabled = false,
  size = '',
  type = 'button',
  ...rest
}: FilledButtonProps) => {
  return (
    <button
      onClick={onClick || onClickEvent}
      disabled={disabled}
      type={type}
      className={`
        ${size || 'px-[20px] py-[10px]'}
        bg-${theme.myBlue} text-${
        theme.myWhite
      } rounded-[10px] border border-transparent
        font-semibold
        hover:bg-${theme.myBlue} hover:border-transparent
        focus:outline-none
        disabled:opacity-50 disabled:hover:bg-${
          theme.myBlue
        } disabled:hover:opacity-50 disabled:cursor-not-allowed
        transition-colors
        font-pretendard
      `}
      {...rest}
    >
      {label}
    </button>
  );
};

export default FilledButton;
