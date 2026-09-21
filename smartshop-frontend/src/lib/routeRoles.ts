export const ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  SHOP_ADMIN: 'SHOP_ADMIN',
  MANAGER: 'MANAGER',
  CASHIER: 'CASHIER',
  ACCOUNTANT: 'ACCOUNTANT',
  INVENTORY_STAFF: 'INVENTORY_STAFF',
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];

export interface RouteAccess {
  roles: Role[];
  notRoles?: Role[];
}

export const ROUTE_ACCESS: Record<string, RouteAccess> = {
  '/dashboard': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.ACCOUNTANT] },
  '/reports': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.ACCOUNTANT] },
  '/recommendations': {
    roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.ACCOUNTANT, ROLES.INVENTORY_STAFF],
  },
  '/pos': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER] },
  '/products': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  '/categories': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER] },
  '/suppliers': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  '/purchases': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF, ROLES.ACCOUNTANT] },
  '/sales': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER, ROLES.ACCOUNTANT] },
  '/returns': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER] },
  '/purchase-returns': {
    roles: [
      ROLES.SUPER_ADMIN,
      ROLES.SHOP_ADMIN,
      ROLES.MANAGER,
      ROLES.INVENTORY_STAFF,
      ROLES.ACCOUNTANT,
    ],
  },
  '/customers': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER] },
  '/inventory': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  '/transfers': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  /**
   * `StockMovementController` is `hasAnyRole('SHOP_ADMIN','MANAGER','INVENTORY_STAFF','ACCOUNTANT')`
   * — it deliberately excludes SUPER_ADMIN. Because `hasRole()` grants SUPER_ADMIN every role,
   * the only way to mirror the backend is an explicit `notRoles`; without it a super-admin would
   * reach a page whose every request 403s.
   */
  '/stock-movements': {
    roles: [ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF, ROLES.ACCOUNTANT],
    notRoles: [ROLES.SUPER_ADMIN],
  },
  '/expenses': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.ACCOUNTANT] },
  '/users': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/role-assignments': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/branches': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/audit-logs': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/settings': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/settings/taxes': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  /** Every authenticated user can view and edit their own profile. */
  '/profile': { roles: [] },
  '/shops': { roles: [ROLES.SUPER_ADMIN] },
  '/admin/shop-approvals': { roles: [ROLES.SUPER_ADMIN] },
  '/admin/dashboard': { roles: [ROLES.SUPER_ADMIN] },
  '/shop/invite-code': { roles: [ROLES.SHOP_ADMIN] },
  '/shop/staff-requests': { roles: [ROLES.SHOP_ADMIN] },
  '/shop-registration': { roles: [], notRoles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/join-shop': { roles: [], notRoles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
};

/**
 * Calculates the best landing route for a user based on their active role assignments.
 * Newly registered users with no shop/roles are guided directly to Shop Registration.
 */
export function getDefaultRoute(roles?: string[]): string {
  if (!roles || roles.length === 0) {
    return '/shop-registration';
  }
  if (roles.includes(ROLES.SUPER_ADMIN)) {
    return '/admin/dashboard';
  }
  if (roles.some((r) => ([ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.ACCOUNTANT] as Role[]).includes(r as Role))) {
    return '/dashboard';
  }
  if (roles.includes(ROLES.CASHIER)) {
    return '/pos';
  }
  if (roles.includes(ROLES.INVENTORY_STAFF)) {
    return '/inventory';
  }
  return '/profile';
}