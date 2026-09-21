import { describe, it, expect } from 'vitest';
import { ROLES, ROUTE_ACCESS } from './routeRoles';

const ROUTE_PATHS = [
  '/dashboard',
  '/reports',
  '/recommendations',
  '/pos',
  '/products',
  '/categories',
  '/suppliers',
  '/customers',
  '/purchases',
  '/sales',
  '/returns',
  '/purchase-returns',
  '/inventory',
  '/transfers',
  '/stock-movements',
  '/expenses',
  '/users',
  '/role-assignments',
  '/branches',
  '/shops',
  '/audit-logs',
  '/profile',
  '/settings',
  '/settings/taxes',
  '/shop-registration',
  '/admin/shop-approvals',
  '/admin/dashboard',
  '/shop/invite-code',
  '/join-shop',
  '/shop/staff-requests',
];

describe('ROUTE_ACCESS', () => {
  it('defines access for every route declared in App.tsx', () => {
    for (const path of ROUTE_PATHS) {
      expect(ROUTE_ACCESS[path], `missing access rules for ${path}`).toBeDefined();
    }
  });

  it('declares no access rules for routes that do not exist', () => {
    const declared = new Set(ROUTE_PATHS);
    for (const path of Object.keys(ROUTE_ACCESS)) {
      expect(declared.has(path), `ROUTE_ACCESS has a stale entry for ${path}`).toBe(true);
    }
  });

  it('restricts admin routes to SUPER_ADMIN only', () => {
    expect(ROUTE_ACCESS['/admin/shop-approvals'].roles).toEqual([ROLES.SUPER_ADMIN]);
    expect(ROUTE_ACCESS['/admin/dashboard'].roles).toEqual([ROLES.SUPER_ADMIN]);
    expect(ROUTE_ACCESS['/shops'].roles).toEqual([ROLES.SUPER_ADMIN]);
  });

  /**
   * `StockMovementController` is granted to SHOP_ADMIN/MANAGER/INVENTORY_STAFF/ACCOUNTANT
   * and denies SUPER_ADMIN. Since `hasRole()` treats SUPER_ADMIN as holding every role, the
   * `notRoles` entry is the only thing keeping super-admins off a page that always 403s.
   */
  it('keeps SUPER_ADMIN off the stock ledger to mirror the backend', () => {
    const access = ROUTE_ACCESS['/stock-movements'];
    expect(access.roles).not.toContain(ROLES.SUPER_ADMIN);
    expect(access.notRoles).toEqual([ROLES.SUPER_ADMIN]);
  });

  it('opens the profile page to every authenticated role', () => {
    expect(ROUTE_ACCESS['/profile'].roles).toEqual([]);
    expect(ROUTE_ACCESS['/profile'].notRoles).toBeUndefined();
  });

  it('gates role assignment to shop and platform administrators', () => {
    expect(ROUTE_ACCESS['/role-assignments'].roles).toEqual([ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]);
    expect(ROUTE_ACCESS['/branches'].roles).toEqual([ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]);
    expect(ROUTE_ACCESS['/audit-logs'].roles).toEqual([ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]);
  });

  it('restricts shop-owner routes to SHOP_ADMIN only', () => {
    expect(ROUTE_ACCESS['/shop/invite-code'].roles).toEqual([ROLES.SHOP_ADMIN]);
    expect(ROUTE_ACCESS['/shop/staff-requests'].roles).toEqual([ROLES.SHOP_ADMIN]);
  });

  it('gates tax settings to shop administrators', () => {
    expect(ROUTE_ACCESS['/settings/taxes'].roles).toEqual([ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]);
  });

  it('only references known roles', () => {
    const known = new Set(Object.values(ROLES));
    for (const access of Object.values(ROUTE_ACCESS)) {
      for (const role of access.roles) {
        expect(known.has(role), `unknown role ${role}`).toBe(true);
      }
      for (const role of access.notRoles ?? []) {
        expect(known.has(role), `unknown role ${role}`).toBe(true);
      }
    }
  });
});