import type { ReactNode } from 'react';
import { useAuthStore } from '../../stores/authStore';
import type { Role } from '../../lib/routeRoles';

export interface CanProps {
  roles?: Role[];
  notRoles?: Role[];
  children: ReactNode;
  fallback?: ReactNode;
}

export function useCan(): (args?: { roles?: Role[]; notRoles?: Role[] }) => boolean {
  const hasRole = useAuthStore((s) => s.hasRole);
  return ({ roles = [], notRoles = [] } = {}) => {
    const granted = roles.length === 0 || hasRole(...roles);
    const excluded = (notRoles ?? []).some((r) => hasRole(r));
    return granted && !excluded;
  };
}

export default function Can({ roles = [], notRoles = [], children, fallback = null }: CanProps) {
  const can = useCan();
  return <>{can({ roles, notRoles }) ? children : fallback}</>;
}