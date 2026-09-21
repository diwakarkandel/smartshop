import { useState, useEffect } from 'react';
import { useNavigate, useLocation, NavLink, Outlet } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Box,
  Typography,
  IconButton,
  Avatar,
  Menu,
  MenuItem,
  Divider,
} from '@mui/material';
import StorefrontIcon from '@mui/icons-material/Storefront';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import InventoryIcon from '@mui/icons-material/Inventory2';
import CategoryIcon from '@mui/icons-material/Category';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import GroupsIcon from '@mui/icons-material/Groups';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import CurrencyExchangeIcon from '@mui/icons-material/CurrencyExchange';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import SwapHorizIcon from '@mui/icons-material/SwapHoriz';
import PeopleIcon from '@mui/icons-material/People';
import SettingsIcon from '@mui/icons-material/Settings';
import PercentIcon from '@mui/icons-material/Percent';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import InsightsIcon from '@mui/icons-material/Insights';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import AppRegistrationIcon from '@mui/icons-material/AppRegistration';
import GroupAddIcon from '@mui/icons-material/GroupAdd';
import AssessmentIcon from '@mui/icons-material/Assessment';
import AssignmentReturnIcon from '@mui/icons-material/AssignmentReturn';
import HistoryIcon from '@mui/icons-material/History';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import DomainIcon from '@mui/icons-material/Domain';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuthStore } from '../../stores/authStore';
import { useShopStore } from '../../stores/shopStore';
import api from '../../lib/api';
import { useQuery } from '@tanstack/react-query';
import { ROUTE_ACCESS } from '../../lib/routeRoles';
import { GRADIENTS } from '../../theme';
import NotificationBell from './NotificationBell';
import type { Branch, PageResponse, Shop } from '../../types';

const DRAWER_WIDTH = 240;

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

/**
 * Grouped so the drawer stays readable as the route table grows. Visibility is
 * still driven entirely by `ROUTE_ACCESS` — a section renders only once at least
 * one of its items survives the role filter.
 */
const NAV_SECTIONS: NavSection[] = [
  {
    title: 'Overview',
    items: [
      { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
      { label: 'Reports', path: '/reports', icon: <AssessmentIcon /> },
      { label: 'Recommendations', path: '/recommendations', icon: <InsightsIcon /> },
    ],
  },
  {
    title: 'Sell',
    items: [
      { label: 'POS', path: '/pos', icon: <PointOfSaleIcon /> },
      { label: 'Sales', path: '/sales', icon: <ReceiptLongIcon /> },
      { label: 'Sale Returns', path: '/returns', icon: <CurrencyExchangeIcon /> },
      { label: 'Customers', path: '/customers', icon: <GroupsIcon /> },
    ],
  },
  {
    title: 'Purchasing',
    items: [
      { label: 'Purchases', path: '/purchases', icon: <ShoppingCartIcon /> },
      { label: 'Purchase Returns', path: '/purchase-returns', icon: <AssignmentReturnIcon /> },
      { label: 'Suppliers', path: '/suppliers', icon: <LocalShippingIcon /> },
    ],
  },
  {
    title: 'Catalogue',
    items: [
      { label: 'Products', path: '/products', icon: <InventoryIcon /> },
      { label: 'Categories', path: '/categories', icon: <CategoryIcon /> },
    ],
  },
  {
    title: 'Stock',
    items: [
      { label: 'Inventory', path: '/inventory', icon: <InventoryIcon /> },
      { label: 'Transfers', path: '/transfers', icon: <SwapHorizIcon /> },
      { label: 'Stock Ledger', path: '/stock-movements', icon: <HistoryIcon /> },
    ],
  },
  {
    title: 'Finance',
    items: [
      { label: 'Expenses', path: '/expenses', icon: <AccountBalanceWalletIcon /> },
      { label: 'Tax Settings', path: '/settings/taxes', icon: <PercentIcon /> },
    ],
  },
  {
    title: 'Administration',
    items: [
      { label: 'Users', path: '/users', icon: <PeopleIcon /> },
      { label: 'Role Assignments', path: '/role-assignments', icon: <ManageAccountsIcon /> },
      { label: 'Branches', path: '/branches', icon: <AccountTreeIcon /> },
      { label: 'Audit Logs', path: '/audit-logs', icon: <FactCheckIcon /> },
      { label: 'Settings', path: '/settings', icon: <SettingsIcon /> },
    ],
  },
  {
    title: 'Platform',
    items: [
      { label: 'Shops', path: '/shops', icon: <DomainIcon /> },
      { label: 'Shop Approvals', path: '/admin/shop-approvals', icon: <AdminPanelSettingsIcon /> },
      { label: 'Oversight', path: '/admin/dashboard', icon: <InsightsIcon /> },
    ],
  },
  {
    title: 'My Shop',
    items: [
      { label: 'Invite Code', path: '/shop/invite-code', icon: <VpnKeyIcon /> },
      { label: 'Staff Requests', path: '/shop/staff-requests', icon: <PendingActionsIcon /> },
    ],
  },
  {
    title: 'Account',
    items: [
      { label: 'My Profile', path: '/profile', icon: <AccountCircleIcon /> },
      { label: 'Shop Registration', path: '/shop-registration', icon: <AppRegistrationIcon /> },
      { label: 'Join a Shop', path: '/join-shop', icon: <GroupAddIcon /> },
    ],
  },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, setSession, hasRole } = useAuthStore();
  const { shopId, branchId, setShop, setBranch } = useShopStore();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const visibleSections = NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => {
      const access = ROUTE_ACCESS[item.path];
      if (!access) return false;
      const excluded = (access.notRoles ?? []).some((r) => hasRole(r));
      return (access.roles.length === 0 || hasRole(...access.roles)) && !excluded;
    }),
  })).filter((section) => section.items.length > 0);

  const grants = user?.branchRoles ?? [];
  const grantShopIds = [...new Set(grants.map((g) => g.shopId).filter(Boolean))];
  const isSuperAdmin = hasRole('SUPER_ADMIN');

  // If the user is SUPER_ADMIN, fetch all shops so they can pick any shop in the top bar.
  const { data: allShops } = useQuery({
    queryKey: ['all-shops-for-superadmin'],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Shop> }>('/shops', {
        params: { page: 0, size: 100 },
      });
      return res.data.data.content;
    },
    enabled: isSuperAdmin,
  });

  const shopList: { id: string; name: string }[] = isSuperAdmin
    ? (allShops ?? []).map((s) => ({ id: s.id, name: s.name }))
    : grantShopIds.map((id) => ({ id, name: `Shop ${id.slice(0, 8)}` }));

  // Fetch branches for the selected shop from the API.
  // /branches returns a plain array: ApiResponse<Branch[]>
  const { data: fetchedBranches } = useQuery({
    queryKey: ['branches-for-layout', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Branch[] }>('/branches', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const branches = fetchedBranches ?? [];

  // Auto-initialize shopId on first load if not yet set
  useEffect(() => {
    if (!shopId && shopList.length > 0) {
      setShop(shopList[0].id);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, shopList]);

  // Auto-initialize branchId whenever branches load and none is selected
  useEffect(() => {
    if (branches.length > 0 && (!branchId || !branches.some((b) => b.id === branchId))) {
      setBranch(branches[0].id);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branches]);

  const handleLogout = async () => {
    try {
      await api.post('/auth/logout', {}, { withCredentials: true });
    } catch {
      // ignore
    }
    setSession(null);
    setShop(null);
    setBranch(null);
    navigate('/login');
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            backgroundImage: GRADIENTS.sidebar,
            color: '#fff',
            border: 'none',
            display: 'flex',
            flexDirection: 'column',
          },
        }}
      >
        <Toolbar sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 36,
              height: 36,
              borderRadius: 2,
              background: 'rgba(255,255,255,0.12)',
              boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.15)',
            }}
          >
            <StorefrontIcon sx={{ color: '#E8DFCA' }} />
          </Box>
          <Typography
            variant="h6"
            sx={{
              fontWeight: 800,
              letterSpacing: '-0.01em',
              background: 'linear-gradient(90deg, #FFFFFF 0%, #E8DFCA 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            SmartShop
          </Typography>
        </Toolbar>
        <Divider sx={{ borderColor: 'rgba(255,255,255,0.15)' }} />
        <Box
          sx={{
            flexGrow: 1,
            overflowY: 'auto',
            '&::-webkit-scrollbar': { width: 6 },
            '&::-webkit-scrollbar-thumb': {
              backgroundColor: 'rgba(255,255,255,0.24)',
              borderRadius: 3,
            },
          }}
        >
          {visibleSections.map((section) => (
            <List
              key={section.title}
              dense
              disablePadding
              subheader={
                <ListSubheader
                  disableSticky
                  sx={{
                    backgroundColor: 'transparent',
                    color: '#E8DFCA',
                    fontSize: 11,
                    fontWeight: 700,
                    letterSpacing: 1,
                    textTransform: 'uppercase',
                    lineHeight: '32px',
                    mt: 1,
                  }}
                >
                  {section.title}
                </ListSubheader>
              }
            >
              {section.items.map((item) => (
                <ListItemButton
                  key={item.path}
                  component={NavLink}
                  to={item.path}
                  selected={location.pathname === item.path}
                  sx={{
                    mx: 1,
                    my: 0.25,
                    position: 'relative',
                    color: 'rgba(255,255,255,0.82)',
                    '&::before': {
                      content: '""',
                      position: 'absolute',
                      left: 0,
                      top: '50%',
                      transform: 'translateY(-50%) scaleY(0)',
                      width: 3,
                      height: '58%',
                      borderRadius: 4,
                      backgroundColor: '#E8DFCA',
                      transition: 'transform 0.22s cubic-bezier(0.22,1,0.36,1)',
                    },
                    '&.Mui-selected': {
                      backgroundColor: 'rgba(255,255,255,0.16)',
                      color: '#fff',
                      '&::before': { transform: 'translateY(-50%) scaleY(1)' },
                      '& .MuiListItemIcon-root': { color: '#E8DFCA' },
                      '&:hover': { backgroundColor: 'rgba(255,255,255,0.22)' },
                    },
                    '&:hover': {
                      backgroundColor: 'rgba(255,255,255,0.09)',
                      transform: 'translateX(3px)',
                    },
                  }}
                >
                  <ListItemIcon sx={{ color: 'inherit', minWidth: 36, transition: 'color 0.2s' }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: 14, fontWeight: 600 }} />
                </ListItemButton>
              ))}
            </List>
          ))}
        </Box>
      </Drawer>

      <Box sx={{ flexGrow: 1 }}>
        <AppBar position="sticky" color="inherit" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Toolbar sx={{ justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              {shopList.length > 0 && (
                <Typography
                  component="select"
                  value={shopId ?? ''}
                  onChange={(e) => {
                    setShop(e.target.value || null);
                    // Clear branchId — the useEffect on fetched branches will
                    // auto-select the first branch for the new shop.
                    setBranch(null);
                  }}
                  sx={{
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    padding: '4px 8px',
                    fontSize: 14,
                  }}
                >
                  {shopList.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </Typography>
              )}
              {branches.length > 0 && (
                <Typography
                  component="select"
                  value={branchId ?? ''}
                  onChange={(e) => setBranch(e.target.value || null)}
                  sx={{
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    padding: '4px 8px',
                    fontSize: 14,
                  }}
                >
                  {branches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
                </Typography>
              )}
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <NotificationBell />
              <IconButton onClick={(e) => setAnchorEl(e.currentTarget)}>
                <Avatar
                  sx={{
                    width: 36,
                    height: 36,
                    color: '#fff',
                    backgroundImage: GRADIENTS.brand,
                    boxShadow: '0 2px 8px rgba(27,67,50,0.28)',
                    transition: 'transform 0.2s ease, box-shadow 0.2s ease',
                    '&:hover': { transform: 'scale(1.06)', boxShadow: '0 4px 14px rgba(27,67,50,0.4)' },
                  }}
                >
                  {user?.fullName?.[0] ?? 'U'}
                </Avatar>
              </IconButton>
            </Box>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
              <MenuItem disabled>
                <Typography variant="body2">{user?.fullName}</Typography>
              </MenuItem>
              <Divider />
              <MenuItem
                onClick={() => {
                  setAnchorEl(null);
                  navigate('/profile');
                }}
              >
                <ListItemIcon>
                  <AccountCircleIcon fontSize="small" />
                </ListItemIcon>
                My Profile
              </MenuItem>
              <MenuItem onClick={handleLogout}>
                <ListItemIcon>
                  <LogoutIcon fontSize="small" />
                </ListItemIcon>
                Logout
              </MenuItem>
            </Menu>
          </Toolbar>
        </AppBar>
        <Box component="main" sx={{ p: 3 }}>
          {shopId ? null : (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              No shop selected. Some pages may require selecting a shop from the top bar.
            </Typography>
          )}
          {/* Keyed by route so each page animates in on navigation. */}
          <Box key={location.pathname} className="ss-page">
            <Outlet />
          </Box>
        </Box>
      </Box>
    </Box>
  );
}