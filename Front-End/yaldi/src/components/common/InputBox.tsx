import React from 'react';
import { theme } from '../../styles/theme';

interface InputBoxProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'size'> {
  size?: string;
}

const InputBox = ({ size = '', className = '', ...rest }: InputBoxProps) => {
  return (
    <input
      type="text"
      className={`
        ${size || 'px-[20px] py-[10px]'}
        rounded-[10px]
        border-2 border-${theme.myBorder}
        focus:outline-none focus:border-${theme.myBlue}
        font-pretendard
        ${className}
      `}
      {...rest}
    />
  );
};

export default InputBox;
