import { describe, it, expect } from 'vitest';
import { ROLES, ROUTE_ACCESS } from './routeRoles';

const ROUTE_PATHS = [
  '/dashboard',
  '/pos',
  '/products',
  '/categories',
  '/suppliers',
  '/customers',
  '/purchases',
  '/sales',
  '/returns',
  '/inventory',
  '/transfers',
  '/expenses',
  '/users',
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

  it('restricts admin routes to SUPER_ADMIN only', () => {
    expect(ROUTE_ACCESS['/admin/shop-approvals'].roles).toEqual([ROLES.SUPER_ADMIN]);
    expect(ROUTE_ACCESS['/admin/dashboard'].roles).toEqual([ROLES.SUPER_ADMIN]);
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