import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { useAuthStore } from './authStore';

interface ShopState {
  shopId: string | null;
  branchId: string | null;
  setShop: (shopId: string | null) => void;
  setBranch: (branchId: string | null) => void;
  reset: () => void;
}

export const useShopStore = create<ShopState>()(
  persist(
    (set) => ({
      shopId: null,
      branchId: null,
      setShop: (shopId) => set({ shopId }),
      setBranch: (branchId) => set({ branchId }),
      reset: () => set({ shopId: null, branchId: null }),
    }),
    { name: 'smartshop-shop' },
  ),
);

export function defaultShopId(): string | null {
  const stored = useShopStore.getState().shopId;
  if (stored) return stored;
  const grants = useAuthStore.getState().user?.branchRoles ?? [];
  const shops = [...new Set(grants.map((g) => g.shopId).filter(Boolean))];
  return shops[0] ?? null;
}

export function defaultBranchId(): string | null {
  const stored = useShopStore.getState().branchId;
  if (stored) return stored;
  const grants = useAuthStore.getState().user?.branchRoles ?? [];
  return grants.find((g) => g.branchId)?.branchId ?? null;
}