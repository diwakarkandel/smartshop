import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Card,
  Grid,
  Typography,
  CircularProgress,
  Alert,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
} from '@mui/material';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
} from 'recharts';
import { alpha } from '@mui/material/styles';
import api from '../lib/api';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import type { DashboardData } from '../types';

const COLORS = ['#1B4332', '#2D6A4F', '#B45309', '#475569', '#B91C1C'];

function formatMoney(n: number) {
  return new Intl.NumberFormat('en-NP', { style: 'currency', currency: 'NPR' }).format(n ?? 0);
}

export default function DashboardPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const today = new Date().toISOString().slice(0, 10);

  const { data, isLoading, error } = useQuery({
    queryKey: ['dashboard', shopId, branchId, today],
    queryFn: async () => {
      const res = await api.get<{ data: DashboardData }>('/reports/dashboard', {
        params: { shopId, branchId: branchId ?? undefined, date: today },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  if (!shopId) {
    return (
      <Box sx={{ py: 4 }}>
        <Alert severity="info">
          Your account is ready. Ask your shop admin to grant you access to a shop and branch.
        </Alert>
      </Box>
    );
  }

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }
  if (error || !data) {
    return <Alert severity="error">Failed to load dashboard. {String(error)}</Alert>;
  }

  const pieData = Object.entries(data.paymentBreakdown ?? {}).map(([name, value]) => ({
    name,
    value,
  }));

  const stats = [
    { label: 'Today Sales', value: data.totalSales, icon: <PointOfSaleIcon />, color: '#2E7D32' },
    { label: 'Sales Count', value: data.salesCount, icon: <TrendingUpIcon />, isCount: true, color: '#2D6A4F' },
    { label: 'Today Purchases', value: data.totalPurchases, icon: <ShoppingCartIcon />, color: '#40916C' },
    { label: 'Today Expenses', value: data.totalExpenses, icon: <AccountBalanceWalletIcon />, color: '#B45309' },
    { label: 'Gross Profit', value: data.grossProfit, icon: <TrendingUpIcon />, color: '#1B4332' },
    { label: 'Low Stock Items', value: data.lowStockCount, icon: <ShoppingCartIcon />, isCount: true, color: '#B91C1C' },
  ];

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 3 }}>
        Dashboard
      </Typography>
      <Grid container spacing={2} sx={{ mb: 3 }} className="ss-stagger">
        {stats.map((s) => (
          <Grid item xs={12} sm={6} md={4} key={s.label}>
            <Card sx={{ p: 2.25, position: 'relative' }}>
              <Box
                sx={{
                  position: 'absolute',
                  inset: 0,
                  opacity: 0.06,
                  background: `radial-gradient(120px 80px at 100% 0%, ${s.color}, transparent 70%)`,
                  pointerEvents: 'none',
                }}
              />
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, position: 'relative' }}>
                <Box
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: 2.5,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#fff',
                    background: `linear-gradient(135deg, ${s.color} 0%, ${alpha(s.color, 0.72)} 100%)`,
                    boxShadow: `0 6px 16px ${alpha(s.color, 0.32)}`,
                  }}
                >
                  {s.icon}
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    {s.label}
                  </Typography>
                  <Typography variant="h6" fontWeight={800}>
                    {s.isCount ? s.value : formatMoney(s.value)}
                  </Typography>
                </Box>
              </Box>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card sx={{ p: 2 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Top Products Today
            </Typography>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data.topProducts ?? []}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="productName" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="quantitySold" fill="#1B4332" />
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card sx={{ p: 2 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Payment Methods
            </Typography>
            {pieData.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No payments recorded today.
              </Typography>
            ) : (
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" outerRadius={100} label>
                    {pieData.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            )}
          </Card>
        </Grid>
      </Grid>

      <Card sx={{ mt: 3, p: 2 }}>
        <Typography variant="h6" sx={{ mb: 2 }}>
          Top Selling Products
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Product</TableCell>
              <TableCell>SKU</TableCell>
              <TableCell align="right">Qty</TableCell>
              <TableCell align="right">Revenue</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(data.topProducts ?? []).map((p) => (
              <TableRow key={p.productId}>
                <TableCell>{p.productName}</TableCell>
                <TableCell>{p.sku}</TableCell>
                <TableCell align="right">{p.quantitySold}</TableCell>
                <TableCell align="right">{formatMoney(p.revenue)}</TableCell>
              </TableRow>
            ))}
            {(data.topProducts ?? []).length === 0 && (
              <TableRow>
                <TableCell colSpan={4}>
                  <Chip label="No sales today" variant="outlined" />
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </Box>
  );
}