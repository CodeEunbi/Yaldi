# 📁 components

## 재사용 가능한 UI 컴포넌트

> Button, Input, Modal 등 재사용 가능한 React 컴포넌트

### commons

```tsx
// Button.tsx
import React from "react";

interface ButtonProps {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  size?: string;
}

const Button = ({ label, onClick, disabled }: ButtonProps) => {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`
        ${size}
        bg-blue-500 text-white rounded 
        disabled:opacity-50 disabled:cursor-not-allowed
      `}
    >
      {label}
    </button>
  );
};

export default Button;
```

→ 이런 공통 컴포넌트들은 색상, 내부 패딩, 폰트, round 처리 등만 통일하고 크기는 설정 x

### layouts

> 화면의 일부로써 반복적으로 사용되는 컴포넌트

```tsx
// Header.tsx

import React from "react";
import { Link } from "react-router-dom";

const Header = () => {
  return (
    <header
      style={{
        padding: "16px",
        backgroundColor: "#f0f0f0",
        borderBottom: "1px solid #ddd",
      }}
    >
      <nav
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        <h1>MyApp</h1>
        <div>
          <Link to="/" style={{ marginRight: "10px" }}>
            Home
          </Link>
          <Link to="/dashboard" style={{ marginRight: "10px" }}>
            Dashboard
          </Link>
          <Link to="/login">Login</Link>
        </div>
      </nav>
    </header>
  );
};

export default Header;
```
