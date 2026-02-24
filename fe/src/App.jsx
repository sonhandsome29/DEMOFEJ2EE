import AuthPanel from './pages/AuthPanel';
import CoursesPanel from './pages/CoursesPanel';
import CheckoutPanel from './pages/CheckoutPanel';
import AdminOrdersPanel from './pages/AdminOrdersPanel';
import { useAuthStore } from './store/useAuthStore';
import { useCartStore } from './store/useCartStore';
import { useCoursesStore } from './store/useCoursesStore';

export default function App() {
  const auth = useAuthStore();
  const cartStore = useCartStore();
  const coursesStore = useCoursesStore();

  return (
    <div className="container">
      <h1>React FE - Backend Security Refactor Integration</h1>
      <p>
        Configure backend URL via <code>VITE_API_BASE_URL</code>. Default: <code>http://localhost:8080/api</code>
      </p>

      <div className="nav">
        <a href="#auth">Auth</a>
        <a href="#courses">Courses</a>
        <a href="#checkout">Checkout</a>
        <a href="#admin-orders">Admin Orders</a>
      </div>

      <AuthPanel auth={auth} />
      <CoursesPanel coursesStore={coursesStore} cartStore={cartStore} />
      <CheckoutPanel cartStore={cartStore} />
      <AdminOrdersPanel />
    </div>
  );
}
