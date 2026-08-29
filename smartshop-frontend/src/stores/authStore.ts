import axios from 'axios';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthUser } from '../types';

export type AuthStatus = 'idle' | 'hydrating' | 'ready';

interface AuthState {
  user: AuthUser | null;
  status: AuthStatus;
  setSession: (auth: AuthUser | null) => void;
  initializeSession: () => Promise<void>;
  hasRole: (...roles: string[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      status: 'idle',
      setSession: (auth) => set({ user: auth, status: 'ready' }),
      initializeSession: async () => {
        if (!get().user) {
          set({ status: 'ready' });
          return;
        }
        set({ status: 'hydrating' });
        try {
          const res = await axios.post<{ data: AuthUser }>(
            '/api/v1/auth/refresh',
            {},
            { withCredentials: true },
          );
          set({ user: res.data.data, status: 'ready' });
        } catch {
          set({ user: null, status: 'ready' });
        }
      },
      hasRole: (...roles) => {
        const u = get().user;
        if (!u) return false;
        if (u.roles.includes('SUPER_ADMIN')) return true;
        return u.roles.some((r) => roles.includes(r));
      },
    }),
    {
      name: 'smartshop-auth',
      partialize: (state) => {
        if (!state.user) return { user: null };
        const { accessToken: _accessToken, ...profile } = state.user;
        return { user: profile };
      },
    },
  ),
);