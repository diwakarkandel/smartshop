import { describe, it, expect, beforeEach, vi } from 'vitest';
import axios from 'axios';
import { useAuthStore } from './authStore';

vi.mock('axios', () => ({
  default: {
    post: vi.fn(),
  },
}));

const authUser = {
  accessToken: 'secret-access-token',
  tokenType: 'Bearer',
  userId: 'u1',
  email: 'admin@shop.com',
  fullName: 'Shop Admin',
  roles: ['SHOP_ADMIN'],
  branchRoles: [],
};

describe('authStore token storage', () => {
  beforeEach(() => {
    localStorage.clear();
    useAuthStore.getState().setSession(null);
    useAuthStore.setState({ status: 'idle' });
    vi.mocked(axios.post).mockReset();
  });

  it('never persists the access token to localStorage', () => {
    useAuthStore.getState().setSession(authUser);
    const raw = localStorage.getItem('smartshop-auth');
    expect(raw).not.toBeNull();
    expect(raw).not.toContain('secret-access-token');
    expect(raw).toContain('admin@shop.com');
    expect(useAuthStore.getState().user?.accessToken).toBe('secret-access-token');
  });

  it('silently refreshes the access token on boot using the HTTP-only cookie', async () => {
    vi.mocked(axios.post).mockResolvedValueOnce({ data: { data: authUser } } as never);
    await useAuthStore.getState().initializeSession();
    expect(axios.post).toHaveBeenCalledWith(
      '/api/v1/auth/refresh',
      {},
      { withCredentials: true },
    );
    expect(useAuthStore.getState().user?.accessToken).toBe('secret-access-token');
    expect(useAuthStore.getState().status).toBe('ready');
  });

  it('refreshes even when no user was rehydrated (cleared localStorage, valid cookie)', async () => {
    localStorage.clear();
    vi.mocked(axios.post).mockResolvedValueOnce({ data: { data: authUser } } as never);
    await useAuthStore.getState().initializeSession();
    expect(axios.post).toHaveBeenCalled();
    expect(useAuthStore.getState().user).toEqual(authUser);
  });

  it('clears the user when the refresh fails', async () => {
    vi.mocked(axios.post).mockRejectedValueOnce(new Error('expired'));
    await useAuthStore.getState().initializeSession();
    expect(useAuthStore.getState().user).toBeNull();
    expect(useAuthStore.getState().status).toBe('ready');
  });
});