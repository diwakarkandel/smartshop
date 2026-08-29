import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  CircularProgress, Alert,
} from '@mui/material';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from 'recharts';
import api, { extractErrorMessage } from '../lib/api';
import { useAuthStore } from '../stores/authStore';
import type { ApiResponse, ShopSalesSummary } from '../types';

export default function AdminOversightDashboard() {
  const hasRole = useAuthStore((s) => s.hasRole);

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-shop-sales'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<ShopSalesSummary[]>>('/admin/dashboard/shop-sales');
      return res.data.data;
    },
    enabled: hasRole('SUPER_ADMIN'),
  });

  if (!hasRole('SUPER_ADMIN')) {
    return <Alert severity="error">Access Denied. This page is for Super Admins only.</Alert>;
  }

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Platform Oversight — Total Sales by Shop
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        High-level revenue overview across all registered shops. Individual shop operations are not
        accessible from this dashboard.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {extractErrorMessage(error)}
        </Alert>
      )}

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          <Card sx={{ p: 3, mb: 3 }}>
            {(data ?? []).length === 0 ? (
              <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 4 }}>
                No sales recorded yet.
              </Typography>
            ) : (
              <ResponsiveContainer width="100%" height={320}>
                <BarChart data={data ?? []}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="shopName" tick={{ fontSize: 12 }} interval={0} angle={-15} dy={10} height={60} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip formatter={(value) => Number(value).toLocaleString()} />
                  <Bar dataKey="totalSales" name="Total Sales" fill="#1B4332" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </Card>

          <Card>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Shop</TableCell>
                  <TableCell align="right">Total Sales</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(data ?? []).map((s) => (
                  <TableRow key={s.shopId}>
                    <TableCell>{s.shopName}</TableCell>
                    <TableCell align="right">{Number(s.totalSales).toLocaleString()}</TableCell>
                  </TableRow>
                ))}
                {(data ?? []).length === 0 && (
                  <TableRow>
                    <TableCell colSpan={2}>No shops with sales</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </Card>
        </>
      )}
    </Box>
  );
}
