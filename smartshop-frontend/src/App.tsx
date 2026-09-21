import { Suspense, lazy, useEffect } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import AppLayout from './components/layout/AppLayout';
import RequireRole from './components/guards/RequireRole';
import PageLoader from './components/feedback/PageLoader';
import { ROLES, ROUTE_ACCESS, getDefaultRoute, type Role, type RouteAccess } from './lib/routeRoles';

const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('./pages/ResetPasswordPage'));
const VerifyEmailPage = lazy(() => import('./pages/VerifyEmailPage'));
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
const ShopsPage = lazy(() => import('./pages/ShopsPage'));
const BranchesPage = lazy(() => import('./pages/BranchesPage'));
const StockMovementsPage = lazy(() => import('./pages/StockMovementsPage'));
const PurchaseReturnsPage = lazy(() => import('./pages/PurchaseReturnsPage'));
const AuditLogsPage = lazy(() => import('./pages/AuditLogsPage'));
const RoleAssignmentsPage = lazy(() => import('./pages/RoleAssignmentsPage'));
const ReportsPage = lazy(() => import('./pages/ReportsPage'));
const RecommendationsPage = lazy(() => import('./pages/RecommendationsPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const LandingPage = lazy(() => import('./pages/LandingPage'));

const ROUTES = [
  { path: 'dashboard', Component: DashboardPage },
  { path: 'reports', Component: ReportsPage },
  { path: 'recommendations', Component: RecommendationsPage },
  { path: 'pos', Component: PosPage },
  { path: 'products', Component: ProductsPage },
  { path: 'categories', Component: CategoriesPage },
  { path: 'suppliers', Component: SuppliersPage },
  { path: 'customers', Component: CustomersPage },
  { path: 'purchases', Component: PurchasesPage },
  { path: 'sales', Component: SalesPage },
  { path: 'returns', Component: ReturnsPage },
  { path: 'purchase-returns', Component: PurchaseReturnsPage },
  { path: 'inventory', Component: InventoryPage },
  { path: 'transfers', Component: TransfersPage },
  { path: 'stock-movements', Component: StockMovementsPage },
  { path: 'expenses', Component: ExpensesPage },
  { path: 'users', Component: UsersPage },
  { path: 'role-assignments', Component: RoleAssignmentsPage },
  { path: 'branches', Component: BranchesPage },
  { path: 'shops', Component: ShopsPage },
  { path: 'audit-logs', Component: AuditLogsPage },
  { path: 'profile', Component: ProfilePage },
  { path: 'settings', Component: SettingsPage },
  { path: 'settings/taxes', Component: TaxSettingsPage },
  { path: 'shop-registration', Component: ShopRegistrationPage },
  { path: 'admin/shop-approvals', Component: AdminShopApprovalsPage },
  { path: 'admin/dashboard', Component: AdminOversightDashboard },
  { path: 'shop/invite-code', Component: ShopInviteCodePage },
  { path: 'join-shop', Component: JoinShopPage },
  { path: 'shop/staff-requests', Component: StaffRequestsPage },
];

/**
 * Fail-closed fallback for a route with no `ROUTE_ACCESS` entry: deny rather than
 * dereference `undefined` and blank the entire router.
 *
 * Both halves are load-bearing. `RequireRole` treats an empty `roles` array as
 * allow-all, so the deny needs an unmatchable sentinel rather than `[]`; and
 * `hasRole()` returns true for *any* query once the user holds SUPER_ADMIN, so the
 * sentinel alone would still let a super-admin through. `routeRoles.test.ts` asserts
 * every declared path has a real entry, so this should be unreachable.
 */
const DENY_ALL: RouteAccess = {
  roles: ['__no_such_role__' as Role],
  notRoles: [ROLES.SUPER_ADMIN],
};

function RequireAuth({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user);
  const status = useAuthStore((s) => s.status);
  if (status === 'idle' || status === 'hydrating') {
    return <PageLoader />;
  }
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function RootRedirect() {
  const user = useAuthStore((s) => s.user);
  return <Navigate to={getDefaultRoute(user?.roles)} replace />;
}

/**
 * Public root: signed-in users go straight to their app; everyone else sees the
 * marketing landing page describing what SmartShop is.
 */
function LandingOrRedirect() {
  const user = useAuthStore((s) => s.user);
  const status = useAuthStore((s) => s.status);
  if (status === 'idle' || status === 'hydrating') return <PageLoader />;
  if (user) return <Navigate to={getDefaultRoute(user.roles)} replace />;
  return <LandingPage />;
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
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/not-authorized" element={<NotAuthorizedPage />} />
        <Route path="/" element={<LandingOrRedirect />} />
        <Route
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          {ROUTES.map(({ path, Component }) => {
            const access = ROUTE_ACCESS[`/${path}`] ?? DENY_ALL;
            return (
              <Route
                key={path}
                path={`/${path}`}
                element={
                  <RequireRole roles={access.roles} notRoles={access.notRoles}>
                    <Component />
                  </RequireRole>
                }
              />
            );
          })}
        </Route>
        <Route path="*" element={<RootRedirect />} />
      </Routes>
    </Suspense>
  );
}