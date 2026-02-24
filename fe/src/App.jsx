import { useMemo, useState } from 'react';
import AuthPanel from './pages/AuthPanel';
import CoursesPanel from './pages/CoursesPanel';
import CheckoutPanel from './pages/CheckoutPanel';
import AdminOrdersPanel from './pages/AdminOrdersPanel';
import { useAuthStore } from './store/useAuthStore';
import { useCartStore } from './store/useCartStore';
import { useCoursesStore } from './store/useCoursesStore';

const tabs = [
  { key: 'auth', label: 'Auth' },
  { key: 'courses', label: 'Courses' },
  { key: 'checkout', label: 'Checkout' },
  { key: 'admin', label: 'Admin Orders' },
];

export default function App() {
  const auth = useAuthStore();
  const cartStore = useCartStore();
  const coursesStore = useCoursesStore();
  const [active, setActive] = useState('auth');

  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
  const isConnected = useMemo(() => baseUrl.includes('localhost:8080/api'), [baseUrl]);

  return (
    <div className="container">
      <header className="header">
        <h1>EduLearn • Modern Learning Frontend</h1>
        <p>UI theo phong cách marketplace hiện đại, tích hợp theo security contract backend mới.</p>
        <div className="badges">
          <span className="badge">API: {baseUrl}</span>
          <span className="badge">Status: {isConnected ? 'Ready for local backend' : 'Custom API URL'}</span>
          <span className="badge">Token revocation aware</span>
        </div>
      </header>

      <div className="nav">
        {tabs.map((tab) => (
          <button key={tab.key} className={active === tab.key ? 'active' : ''} onClick={() => setActive(tab.key)}>
            {tab.label}
          </button>
        ))}
      </div>

      <div className="grid">
        {active === 'auth' && <AuthPanel auth={auth} />}
        {active === 'courses' && <CoursesPanel coursesStore={coursesStore} cartStore={cartStore} />}
        {active === 'checkout' && <CheckoutPanel cartStore={cartStore} />}
        {active === 'admin' && <AdminOrdersPanel />}
      </div>
    </div>
  );
}
