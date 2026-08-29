import { Suspense, lazy, useEffect } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import AppLayout from './components/layout/AppLayout';
import RequireRole from './components/guards/RequireRole';
import PageLoader from './components/feedback/PageLoader';
import { ROUTE_ACCESS } from './lib/routeRoles';

const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const NotAuthorizedPage = lazy(() => import('./pages/NotAuthorizedPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const PosPage = lazy(() => import('./pages/PosPage'));
const ProductsPage = lazy(() => import('./pages/ProductsPage'));
const CategoriesPage = lazy(() => import('./pages/CategoriesPage'));
const SuppliersPage = lazy(() => import('./pages/SuppliersPage'));
const CustomersPage = lazy(() => import('./pages/CustomersPage'));
const PurchasesPage = lazy(() => import('./pages/PurchasesPage'));
const SalesPage = lazy(() => import('./pages/SalesPage'));
const ReturnsPage = lazy(() => import('./pages/ReturnsPage'));
const InventoryPage = lazy(() => import('./pages/InventoryPage'));
const TransfersPage = lazy(() => import('./pages/TransfersPage'));
const ExpensesPage = lazy(() => import('./pages/ExpensesPage'));
const UsersPage = lazy(() => import('./pages/UsersPage'));
const SettingsPage = lazy(() => import('./pages/SettingsPage'));
const TaxSettingsPage = lazy(() => import('./pages/TaxSettingsPage'));
const ShopRegistrationPage = lazy(() => import('./pages/ShopRegistrationPage'));
const AdminShopApprovalsPage = lazy(() => import('./pages/AdminShopApprovalsPage'));
const AdminOversightDashboard = lazy(() => import('./pages/AdminOversightDashboard'));
const ShopInviteCodePage = lazy(() => import('./pages/ShopInviteCodePage'));
const JoinShopPage = lazy(() => import('./pages/JoinShopPage'));
const StaffRequestsPage = lazy(() => import('./pages/StaffRequestsPage'));

const ROUTES = [
  { path: 'dashboard', Component: DashboardPage },
  { path: 'pos', Component: PosPage },
  { path: 'products', Component: ProductsPage },
  { path: 'categories', Component: CategoriesPage },
  { path: 'suppliers', Component: SuppliersPage },
  { path: 'customers', Component: CustomersPage },
  { path: 'purchases', Component: PurchasesPage },
  { path: 'sales', Component: SalesPage },
  { path: 'returns', Component: ReturnsPage },
  { path: 'inventory', Component: InventoryPage },
  { path: 'transfers', Component: TransfersPage },
  { path: 'expenses', Component: ExpensesPage },
  { path: 'users', Component: UsersPage },
  { path: 'settings', Component: SettingsPage },
  { path: 'settings/taxes', Component: TaxSettingsPage },
  { path: 'shop-registration', Component: ShopRegistrationPage },
  { path: 'admin/shop-approvals', Component: AdminShopApprovalsPage },
  { path: 'admin/dashboard', Component: AdminOversightDashboard },
  { path: 'shop/invite-code', Component: ShopInviteCodePage },
  { path: 'join-shop', Component: JoinShopPage },
  { path: 'shop/staff-requests', Component: StaffRequestsPage },
];

function RequireAuth({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user);
  const status = useAuthStore((s) => s.status);
  if (status === 'idle' || status === 'hydrating') {
    return <PageLoader />;
  }
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  const initializeSession = useAuthStore((s) => s.initializeSession);

  useEffect(() => {
    void initializeSession();
  }, [initializeSession]);

  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/not-authorized" element={<NotAuthorizedPage />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          {ROUTES.map(({ path, Component }) => {
            const access = ROUTE_ACCESS[`/${path}`];
            return (
              <Route
                key={path}
                path={path}
                element={
                  <RequireRole roles={access.roles} notRoles={access.notRoles}>
                    <Component />
                  </RequireRole>
                }
              />
            );
          })}
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  );
}