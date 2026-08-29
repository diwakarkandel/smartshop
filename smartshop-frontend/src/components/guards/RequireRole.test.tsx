import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RequireRole from './RequireRole';
import { useAuthStore } from '../../stores/authStore';
import { ROLES } from '../../lib/routeRoles';

function renderWithRoutes(ui: React.ReactElement) {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={ui} />
        <Route path="/not-authorized" element={<div>Not Authorized Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

const shopAdminUser = {
  accessToken: 'dummy-token',
  tokenType: 'Bearer',
  userId: 'u1',
  email: 'admin@shop.com',
  fullName: 'Shop Admin',
  roles: [ROLES.SHOP_ADMIN],
  branchRoles: [],
};

describe('RequireRole', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, status: 'ready' });
  });

  it('renders children when the user has an allowed role', () => {
    useAuthStore.setState({ user: shopAdminUser });
    renderWithRoutes(
      <RequireRole roles={[ROLES.SUPER_ADMIN, ROLES.SHOP_ADMIN]}>
        <div>admin panel</div>
      </RequireRole>,
    );
    expect(screen.getByText('admin panel')).toBeInTheDocument();
  });

  it('redirects to /not-authorized when the role does not match', () => {
    useAuthStore.setState({ user: shopAdminUser });
    renderWithRoutes(
      <RequireRole roles={[ROLES.SUPER_ADMIN]}>
        <div>super admin panel</div>
      </RequireRole>,
    );
    expect(screen.getByText('Not Authorized Page')).toBeInTheDocument();
    expect(screen.queryByText('super admin panel')).not.toBeInTheDocument();
  });

  it('blocks roles listed in notRoles', () => {
    useAuthStore.setState({ user: shopAdminUser });
    renderWithRoutes(
      <RequireRole notRoles={[ROLES.SHOP_ADMIN]}>
        <div>anyone but shop admin</div>
      </RequireRole>,
    );
    expect(screen.getByText('Not Authorized Page')).toBeInTheDocument();
  });

  it('allows any authenticated role when no roles are required', () => {
    useAuthStore.setState({ user: shopAdminUser });
    renderWithRoutes(<RequireRole><div>open panel</div></RequireRole>);
    expect(screen.getByText('open panel')).toBeInTheDocument();
  });

  it('SUPER_ADMIN passes roles-gated guards like hasRole', () => {
    useAuthStore.setState({ user: { ...shopAdminUser, roles: [ROLES.SUPER_ADMIN] } });
    renderWithRoutes(
      <RequireRole roles={[ROLES.SHOP_ADMIN]}>
        <div>shop admin panel</div>
      </RequireRole>,
    );
    expect(screen.getByText('shop admin panel')).toBeInTheDocument();
  });
});