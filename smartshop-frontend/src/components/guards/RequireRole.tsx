import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import type { Role } from '../../lib/routeRoles';

interface RequireRoleProps {
  roles?: Role[];
  notRoles?: Role[];
  children: React.ReactNode;
}

export default function RequireRole({
  roles = [],
  notRoles = [],
  children,
}: RequireRoleProps) {
  const user = useAuthStore((s) => s.user);
  const hasRole = useAuthStore((s) => s.hasRole);

  const roleGranted = roles.length === 0 || hasRole(...roles);
  const excluded = (notRoles ?? []).some((r) => user?.roles.includes(r));

  if (!roleGranted || excluded) {
    return <Navigate to="/not-authorized" replace />;
  }
  return <>{children}</>;
}