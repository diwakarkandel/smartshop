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
  '/pos': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER] },
  '/products': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  '/categories': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER] },
  '/suppliers': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  '/purchases': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF, ROLES.ACCOUNTANT] },
  '/sales': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER, ROLES.ACCOUNTANT] },
  '/returns': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER] },
  '/customers': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.CASHIER] },
  '/inventory': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  '/transfers': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.INVENTORY_STAFF] },
  '/expenses': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN, ROLES.MANAGER, ROLES.ACCOUNTANT] },
  '/users': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/settings': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/settings/taxes': { roles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/admin/shop-approvals': { roles: [ROLES.SUPER_ADMIN] },
  '/admin/dashboard': { roles: [ROLES.SUPER_ADMIN] },
  '/shop/invite-code': { roles: [ROLES.SHOP_ADMIN] },
  '/shop/staff-requests': { roles: [ROLES.SHOP_ADMIN] },
  '/shop-registration': { roles: [], notRoles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
  '/join-shop': { roles: [], notRoles: [ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN] },
};