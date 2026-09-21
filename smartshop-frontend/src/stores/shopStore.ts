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

/**
 * Reactive hook — components using this will re-render when shopId changes.
 * Falls back to the first shop from branchRoles if none is persisted.
 */
export function useDefaultShopId(): string | null {
  const stored = useShopStore((s) => s.shopId);
  // Select the raw array reference — never `?? []` inside the selector, which
  // would return a fresh array each render and drive an infinite re-render loop.
  const grants = useAuthStore((s) => s.user?.branchRoles);
  if (stored) return stored;
  const shops = [...new Set((grants ?? []).map((g) => g.shopId).filter(Boolean))];
  return shops[0] ?? null;
}

/**
 * Reactive hook — components using this will re-render when branchId changes.
 * Falls back to the first branch from branchRoles if none is persisted.
 */
export function useDefaultBranchId(): string | null {
  const stored = useShopStore((s) => s.branchId);
  // Select the raw array reference — see the note in useDefaultShopId.
  const grants = useAuthStore((s) => s.user?.branchRoles);
  if (stored) return stored;
  return (grants ?? []).find((g) => g.branchId)?.branchId ?? null;
}

/** @deprecated Use the reactive hook `useDefaultShopId()` inside components. */
export function defaultShopId(): string | null {
  const stored = useShopStore.getState().shopId;
  if (stored) return stored;
  const grants = useAuthStore.getState().user?.branchRoles ?? [];
  const shops = [...new Set(grants.map((g) => g.shopId).filter(Boolean))];
  return shops[0] ?? null;
}

/** @deprecated Use the reactive hook `useDefaultBranchId()` inside components. */
export function defaultBranchId(): string | null {
  const stored = useShopStore.getState().branchId;
  if (stored) return stored;
  const grants = useAuthStore.getState().user?.branchRoles ?? [];
  return grants.find((g) => g.branchId)?.branchId ?? null;
}