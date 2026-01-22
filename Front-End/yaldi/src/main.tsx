import { createRoot } from 'react-dom/client';
import './styles/global.css';
import App from './App';

// App 컴포넌트를 루트 엘리먼트에 렌더링
createRoot(document.getElementById('root')!).render(<App />);
