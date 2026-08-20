import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthUser } from '../types';

interface AuthState {
  user: AuthUser | null;
  setUser: (user: AuthUser | null) => void;
  hasRole: (...roles: string[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      setUser: (user) => set({ user }),
      hasRole: (...roles) => {
        const u = get().user;
        if (!u) return false;
        if (u.roles.includes('SUPER_ADMIN')) return true;
        return u.roles.some((r) => roles.includes(r));
      },
    }),
    { name: 'smartshop-auth' },
  ),
);