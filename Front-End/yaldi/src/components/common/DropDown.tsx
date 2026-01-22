import { useState, useRef, useEffect } from 'react';
import { theme } from '../../styles/theme';
import DownIcon from '../../assets/icons/down_icon.svg?react';
import UpIcon from '../../assets/icons/up_icon.svg?react';

interface DropDownProps {
  size?: string;
  className?: string;
  options: Array<{ value: string; label: string }>;
  value?: string;
  onChange?: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
  name?: string;
}

const DropDown = ({
  size = '',
  className = '',
  options = [],
  value = '',
  onChange,
  disabled = false,
  placeholder = '선택하세요',
  name,
}: DropDownProps) => {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedValue, setSelectedValue] = useState(value);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const [buttonWidth, setButtonWidth] = useState<number | undefined>(undefined);

  useEffect(() => {
    setSelectedValue(value);
  }, [value]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      if (buttonRef.current) {
        setButtonWidth(buttonRef.current.offsetWidth);
      }
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const handleSelect = (optionValue: string) => {
    setSelectedValue(optionValue);
    onChange?.(optionValue);
    setIsOpen(false);
  };

  const selectedLabel =
    options.find((option) => option.value === selectedValue)?.label ||
    placeholder;

  return (
    <div className={`relative ${className}`} ref={dropdownRef}>
      {name && <input type="hidden" name={name} value={selectedValue} />}
      <button
        ref={buttonRef}
        type="button"
        onClick={() => !disabled && setIsOpen(!isOpen)}
        disabled={disabled}
        className={`
          ${size || 'px-[20px] py-[10px]'}
          rounded-[10px]
          border-2 border-${theme.myBorder}
          bg-${theme.myWhite}
          text-left
          flex items-center justify-between
          focus:outline-none focus:border-${theme.myBlue}
          disabled:opacity-50 disabled:cursor-not-allowed
          cursor-pointer
          font-pretendard
        `}
      >
        <span className={selectedValue ? 'text-my-black' : 'text-my-border'}>
          {selectedLabel}
        </span>
        {isOpen ? (
          <UpIcon className="w-5 h-5" />
        ) : (
          <DownIcon className="w-5 h-5" />
        )}
      </button>

      {isOpen && (
        <div
          style={{ width: buttonWidth ? `${buttonWidth}px` : undefined }}
          className={`
            absolute z-50 mt-1
            rounded-[10px]
            border-2 border-${theme.myBorder}
            bg-${theme.myWhite}
            shadow-lg
            max-h-60 overflow-auto
          `}
        >
          {options.map((option) => (
            <div
              key={option.value}
              onClick={() => handleSelect(option.value)}
              className={`
                px-[20px] py-[10px]
                cursor-pointer
                font-pretendard
                ${
                  selectedValue === option.value
                    ? `bg-${theme.myLightBlue}`
                    : 'hover:bg-' + theme.myLightBlue
                }
                transition-colors
              `}
            >
              {option.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default DropDown;
