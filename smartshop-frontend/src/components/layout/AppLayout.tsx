import { useState } from 'react';
import { useNavigate, useLocation, NavLink, Outlet } from 'react-router-dom';
import {
  AppBar,
  Toolbar,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
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
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import InsightsIcon from '@mui/icons-material/Insights';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import AppRegistrationIcon from '@mui/icons-material/AppRegistration';
import GroupAddIcon from '@mui/icons-material/GroupAdd';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuthStore } from '../../stores/authStore';
import { useShopStore } from '../../stores/shopStore';
import api from '../../lib/api';
import { ROUTE_ACCESS } from '../../lib/routeRoles';

const DRAWER_WIDTH = 240;

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { label: 'POS', path: '/pos', icon: <PointOfSaleIcon /> },
  { label: 'Products', path: '/products', icon: <InventoryIcon /> },
  { label: 'Categories', path: '/categories', icon: <CategoryIcon /> },
  { label: 'Purchases', path: '/purchases', icon: <ShoppingCartIcon /> },
  { label: 'Suppliers', path: '/suppliers', icon: <LocalShippingIcon /> },
  { label: 'Sales', path: '/sales', icon: <ReceiptLongIcon /> },
  { label: 'Returns', path: '/returns', icon: <CurrencyExchangeIcon /> },
  { label: 'Customers', path: '/customers', icon: <GroupsIcon /> },
  { label: 'Inventory', path: '/inventory', icon: <InventoryIcon /> },
  { label: 'Transfers', path: '/transfers', icon: <SwapHorizIcon /> },
  { label: 'Expenses', path: '/expenses', icon: <AccountBalanceWalletIcon /> },
  { label: 'Users', path: '/users', icon: <PeopleIcon /> },
  { label: 'Settings', path: '/settings', icon: <SettingsIcon /> },
  { label: 'Shop Approvals', path: '/admin/shop-approvals', icon: <AdminPanelSettingsIcon /> },
  { label: 'Oversight', path: '/admin/dashboard', icon: <InsightsIcon /> },
  { label: 'Invite Code', path: '/shop/invite-code', icon: <VpnKeyIcon /> },
  { label: 'Staff Requests', path: '/shop/staff-requests', icon: <PendingActionsIcon /> },
  { label: 'Shop Registration', path: '/shop-registration', icon: <AppRegistrationIcon /> },
  { label: 'Join a Shop', path: '/join-shop', icon: <GroupAddIcon /> },
];

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, setSession, hasRole } = useAuthStore();
  const { shopId, branchId, setShop, setBranch } = useShopStore();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const visibleItems = NAV_ITEMS.filter((item) => {
    const access = ROUTE_ACCESS[item.path];
    if (!access) return false;
    const excluded = (access.notRoles ?? []).some((r) => user?.roles.includes(r));
    return (access.roles.length === 0 || hasRole(...access.roles)) && !excluded;
  });
  const grants = user?.branchRoles ?? [];
  const shops = [...new Set(grants.map((g) => g.shopId).filter(Boolean))];
  const branches = grants.filter((g) => g.branchId);

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
            backgroundColor: '#1B4332',
            color: '#fff',
          },
        }}
      >
        <Toolbar sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <StorefrontIcon sx={{ color: '#E8DFCA' }} />
          <Typography variant="h6" sx={{ fontWeight: 700 }}>
            SmartShop
          </Typography>
        </Toolbar>
        <Divider sx={{ borderColor: 'rgba(255,255,255,0.15)' }} />
        <List>
          {visibleItems.map((item) => (
            <ListItemButton
              key={item.path}
              component={NavLink}
              to={item.path}
              selected={location.pathname === item.path}
              sx={{
                '&.Mui-selected': {
                  backgroundColor: 'rgba(255,255,255,0.15)',
                  '&:hover': { backgroundColor: 'rgba(255,255,255,0.2)' },
                },
                '&:hover': { backgroundColor: 'rgba(255,255,255,0.08)' },
              }}
            >
              <ListItemIcon sx={{ color: 'inherit', minWidth: 36 }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.label} primaryTypographyProps={{ fontSize: 14 }} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>

      <Box sx={{ flexGrow: 1 }}>
        <AppBar position="sticky" color="inherit" elevation={0} sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Toolbar sx={{ justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', gap: 2 }}>
              {shops.length > 0 && (
                <Typography
                  component="select"
                  value={shopId ?? ''}
                  onChange={(e) => setShop(e.target.value || null)}
                  sx={{
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 1,
                    padding: '4px 8px',
                    fontSize: 14,
                  }}
                >
                  {shops.map((s) => (
                    <option key={s} value={s}>
                      Shop {s.slice(0, 8)}
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
                    <option key={b.branchId} value={b.branchId}>
                      {b.branchName}
                    </option>
                  ))}
                </Typography>
              )}
            </Box>
            <IconButton onClick={(e) => setAnchorEl(e.currentTarget)}>
              <Avatar sx={{ bgcolor: 'primary.main', width: 34, height: 34 }}>
                {user?.fullName?.[0] ?? 'U'}
              </Avatar>
            </IconButton>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
              <MenuItem disabled>
                <Typography variant="body2">{user?.fullName}</Typography>
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
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}