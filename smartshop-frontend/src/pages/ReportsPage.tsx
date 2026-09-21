import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Grid, CircularProgress, Alert,
  TextField, Button, Table, TableHead, TableRow, TableCell, TableBody,
  Divider, Chip, Stack,
} from '@mui/material';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import ReceiptIcon from '@mui/icons-material/Receipt';
import SavingsIcon from '@mui/icons-material/Savings';
import InventoryIcon from '@mui/icons-material/Inventory';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import WarningIcon from '@mui/icons-material/Warning';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import CategoryIcon from '@mui/icons-material/Category';
import { alpha } from '@mui/material/styles';
import api from '../lib/api';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import type {
  ApiResponse,
  DashboardData,
  SalesSummary,
  CategoryProfitReport,
  ExpenseSummaryReport,
  InventoryValuationItem,
} from '../types';
import { PAYMENT_METHODS } from '../types';

const PIE_COLORS = ['#1976d2', '#2e7d32', '#ed6c02', '#9c27b0', '#0288d1', '#d32f2f'];

interface TopProductRow {
  productId: string;
  productName: string;
  sku: string;
  quantitySold: number;
  revenue: number;
}

function downloadCsv(filename: string, headers: string[], rows: (string | number)[][]) {
  const escape = (val: string | number) => `"${String(val ?? '').replace(/"/g, '""')}"`;
  const csvContent = [
    headers.map(escape).join(','),
    ...rows.map((row) => row.map(escape).join(',')),
  ].join('\r\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function StatCard({ title, value, icon, color, subtitle }: {
  title: string; value: string; icon: React.ReactNode; color: string; subtitle?: string;
}) {
  return (
    <Card sx={{ p: 2.25, display: 'flex', alignItems: 'center', gap: 2, height: '100%' }}>
      <Box
        sx={{
          p: 1.5,
          borderRadius: 2.5,
          display: 'flex',
          color: '#fff',
          background: (t) => `linear-gradient(135deg, ${t.palette[color as 'primary'].main} 0%, ${t.palette[color as 'primary'].light} 100%)`,
          boxShadow: (t) => `0 6px 16px ${alpha(t.palette[color as 'primary'].main, 0.3)}`,
        }}
      >
        {icon}
      </Box>
      <Box>
        <Typography variant="body2" color="text.secondary">{title}</Typography>
        <Typography variant="h6" fontWeight={800}>{value}</Typography>
        {subtitle && <Typography variant="caption" color="text.secondary">{subtitle}</Typography>}
      </Box>
    </Card>
  );
}

export default function ReportsPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const today = new Date().toISOString().split('T')[0];
  const firstOfMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];

  const [dateFrom, setDateFrom] = useState(firstOfMonth);
  const [dateTo, setDateTo] = useState(today);
  const [queryDates, setQueryDates] = useState({ from: firstOfMonth, to: today });

  const { data: dashboard, isLoading: dashLoading } = useQuery({
    queryKey: ['dashboard', shopId, branchId, today],
    queryFn: async () => {
      const res = await api.get<ApiResponse<DashboardData>>('/reports/dashboard', {
        params: { shopId, branchId: branchId ?? undefined },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: summary, isLoading: summaryLoading, error: summaryError } = useQuery({
    queryKey: ['sales-summary', shopId, queryDates.from, queryDates.to],
    queryFn: async () => {
      const res = await api.get<ApiResponse<SalesSummary>>('/reports/sales-summary', {
        params: { shopId, dateFrom: queryDates.from, dateTo: queryDates.to },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: categoryProfit, isLoading: catProfitLoading } = useQuery({
    queryKey: ['profit-by-category', shopId, queryDates.from, queryDates.to],
    queryFn: async () => {
      const res = await api.get<ApiResponse<CategoryProfitReport>>('/reports/profit-by-category', {
        params: { shopId, dateFrom: queryDates.from, dateTo: queryDates.to },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: expenseSummary, isLoading: expenseLoading } = useQuery({
    queryKey: ['expense-summary', shopId, queryDates.from, queryDates.to],
    queryFn: async () => {
      const res = await api.get<ApiResponse<ExpenseSummaryReport>>('/reports/expense-summary', {
        params: { shopId, dateFrom: queryDates.from, dateTo: queryDates.to },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: valuation, isLoading: valuationLoading } = useQuery({
    queryKey: ['inventory-valuation', shopId, branchId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<InventoryValuationItem[]>>('/reports/inventory-valuation', {
        params: { shopId, branchId: branchId ?? undefined },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: topProducts, isLoading: topLoading } = useQuery({
    queryKey: ['top-products', shopId, queryDates.from, queryDates.to],
    queryFn: async () => {
      const res = await api.get<ApiResponse<TopProductRow[]>>('/reports/top-products', {
        params: { shopId, dateFrom: queryDates.from, dateTo: queryDates.to, limit: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const totalInventoryValue = (valuation ?? []).reduce((sum, r) => sum + Number(r.totalValue), 0);

  const fmt = (v: number) => 'Rs. ' + Number(v ?? 0).toFixed(2);

  // CSV export handlers
  const handleExportSalesSummary = () => {
    if (!summary) return;
    const rows = [
      ['Date From', summary.dateFrom],
      ['Date To', summary.dateTo],
      ['Total Sales (Rs.)', Number(summary.totalSales).toFixed(2)],
      ['Total VAT (Rs.)', Number(summary.totalVat).toFixed(2)],
      ['Total Discount (Rs.)', Number(summary.totalDiscount).toFixed(2)],
      ['Total COGS (Rs.)', Number(summary.totalCogs).toFixed(2)],
      ['Gross Profit (Rs.)', Number(summary.grossProfit).toFixed(2)],
      ['Total Expenses (Rs.)', Number(summary.totalExpenses ?? 0).toFixed(2)],
      ['Net Profit (Rs.)', Number(summary.netProfit ?? 0).toFixed(2)],
      ['Sale Count', summary.saleCount],
    ];
    downloadCsv(`sales-summary-${queryDates.from}-to-${queryDates.to}.csv`, ['Metric', 'Value'], rows);
  };

  const handleExportCategoryProfit = () => {
    if (!categoryProfit?.categories) return;
    const rows = categoryProfit.categories.map((c) => [
      c.categoryName,
      Number(c.totalQty).toFixed(2),
      Number(c.totalRevenue).toFixed(2),
      Number(c.totalCogs).toFixed(2),
      Number(c.totalProfit).toFixed(2),
      Number(c.profitMarginPct).toFixed(2) + '%',
    ]);
    downloadCsv(`category-profit-${queryDates.from}-to-${queryDates.to}.csv`,
      ['Category', 'Qty Sold', 'Revenue (Rs.)', 'COGS (Rs.)', 'Profit (Rs.)', 'Margin %'],
      rows
    );
  };

  const handleExportExpenses = () => {
    if (!expenseSummary) return;
    const rows = Object.entries(expenseSummary.byCategory ?? {}).map(([cat, amt]) => [
      cat,
      Number(amt).toFixed(2),
      expenseSummary.totalExpenses > 0
        ? ((Number(amt) / Number(expenseSummary.totalExpenses)) * 100).toFixed(2) + '%'
        : '0%',
    ]);
    downloadCsv(`expense-breakdown-${queryDates.from}-to-${queryDates.to}.csv`,
      ['Category', 'Amount (Rs.)', 'Share %'],
      rows
    );
  };

  const handleExportTopProducts = () => {
    if (!topProducts?.length) return;
    const rows = topProducts.map((p) => [
      p.productName,
      p.sku,
      Number(p.quantitySold).toFixed(2),
      Number(p.revenue).toFixed(2),
    ]);
    downloadCsv(`top-products-${queryDates.from}-to-${queryDates.to}.csv`,
      ['Product', 'SKU', 'Qty Sold', 'Revenue (Rs.)'],
      rows
    );
  };

  const handleExportInventory = () => {
    if (!valuation) return;
    const rows = valuation.map((r) => [
      r.productName,
      r.sku,
      r.branchName,
      Number(r.quantityAvailable).toFixed(2),
      Number(r.averageCost).toFixed(2),
      Number(r.totalValue).toFixed(2),
    ]);
    downloadCsv('inventory-valuation.csv',
      ['Product', 'SKU', 'Branch', 'Quantity Available', 'Average Cost (Rs.)', 'Total Value (Rs.)'],
      rows
    );
  };

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 3 }}>Reports & Analytics</Typography>

      <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1.5 }}>{"Today's Snapshot"}</Typography>
      {dashLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress /></Box>
      ) : dashboard ? (
        <Grid container spacing={2} sx={{ mb: 3 }} className="ss-stagger">
          <Grid item xs={12} sm={6} md={2}>
            <StatCard title="Total Sales" value={fmt(dashboard.totalSales)} icon={<TrendingUpIcon />} color="success" />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <StatCard title="Sales Count" value={String(dashboard.salesCount)} icon={<ReceiptIcon />} color="primary" />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <StatCard title="Gross Profit" value={fmt(dashboard.grossProfit)} icon={<SavingsIcon />} color="warning" />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <StatCard title="Net Profit" value={fmt(dashboard.netProfit ?? 0)} icon={<AttachMoneyIcon />} color="success" subtitle="After expenses" />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <StatCard title="Low Stock Items" value={String(dashboard.lowStockCount)} icon={<InventoryIcon />} color="error" />
          </Grid>
          <Grid item xs={12} sm={6} md={2}>
            <StatCard title="Slow Moving" value={String(dashboard.slowMovingCount ?? 0)} icon={<WarningIcon />} color="warning" subtitle="No sales in 90 days" />
          </Grid>
        </Grid>
      ) : null}

      {dashboard && dashboard.topProducts.length > 0 && (
        <>
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>Top Products Today</Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} md={7}>
              <Card>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Product</TableCell>
                      <TableCell>SKU</TableCell>
                      <TableCell align="right">Qty Sold</TableCell>
                      <TableCell align="right">Revenue</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {dashboard.topProducts.map((p) => (
                      <TableRow key={p.productId}>
                        <TableCell>{p.productName}</TableCell>
                        <TableCell>{p.sku}</TableCell>
                        <TableCell align="right">{p.quantitySold}</TableCell>
                        <TableCell align="right">{Number(p.revenue).toFixed(2)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Card>
            </Grid>
            <Grid item xs={12} md={5}>
              <Card sx={{ p: 2, height: '100%' }}>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Revenue by Product</Typography>
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={dashboard.topProducts.map((p) => ({ name: p.productName, revenue: p.revenue }))} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(value) => Number(value).toFixed(2)} />
                    <Bar dataKey="revenue" fill="#1976d2" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </Card>
            </Grid>
          </Grid>
        </>
      )}

      {dashboard && Object.keys(dashboard.paymentBreakdown).length > 0 && (
        <>
          <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>Payment Breakdown Today</Typography>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} md={7}>
              <Card>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Method</TableCell>
                      <TableCell align="right">Amount</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {PAYMENT_METHODS.map((m) => (
                      <TableRow key={m}>
                        <TableCell>{m}</TableCell>
                        <TableCell align="right">{(dashboard.paymentBreakdown[m] ?? 0).toFixed(2)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Card>
            </Grid>
            <Grid item xs={12} md={5}>
              <Card sx={{ p: 2, height: '100%' }}>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Share by Payment Method</Typography>
                {(() => {
                  const pieData = PAYMENT_METHODS.map((m, i) => ({
                    name: m,
                    value: dashboard.paymentBreakdown[m] ?? 0,
                    color: PIE_COLORS[i % PIE_COLORS.length],
                  })).filter((d) => d.value > 0);
                  if (pieData.length === 0) return null;
                  return (
                    <ResponsiveContainer width="100%" height={220}>
                      <PieChart>
                        <Pie data={pieData} dataKey="value" nameKey="name" innerRadius={50} outerRadius={85} paddingAngle={2}>
                          {pieData.map((entry) => (<Cell key={entry.name} fill={entry.color} />))}
                        </Pie>
                        <Tooltip formatter={(value) => Number(value).toFixed(2)} />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>
                  );
                })()}
              </Card>
            </Grid>
          </Grid>
        </>
      )}

      <Divider sx={{ my: 3 }} />

      {/* Period Filter */}
      <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1.5 }}>Period Reports</Typography>
      <Box sx={{ display: 'flex', gap: 2, mb: 3, alignItems: 'center' }}>
        <TextField label="From" type="date" size="small" InputLabelProps={{ shrink: true }} value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
        <TextField label="To" type="date" size="small" InputLabelProps={{ shrink: true }} value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
        <Button variant="contained" onClick={() => setQueryDates({ from: dateFrom, to: dateTo })}>Apply Filter</Button>
      </Box>

      {/* Sales Summary */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="subtitle2" fontWeight="bold">Sales Summary</Typography>
        <Button
          size="small"
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          onClick={handleExportSalesSummary}
          disabled={!summary}
        >
          Export CSV
        </Button>
      </Stack>
      {summaryLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress /></Box>
      ) : summaryError ? (
        <Alert severity="error">Failed to load summary.</Alert>
      ) : summary ? (
        <Card sx={{ p: 2.5, mb: 3 }}>
          <Grid container spacing={2}>
            {[
              { label: 'Total Sales', value: fmt(summary.totalSales) },
              { label: 'Total VAT', value: fmt(summary.totalVat) },
              { label: 'Total Discount', value: fmt(summary.totalDiscount) },
              { label: 'Total COGS', value: fmt(summary.totalCogs) },
              { label: 'Gross Profit', value: fmt(summary.grossProfit) },
              { label: 'Total Expenses', value: fmt(summary.totalExpenses ?? 0) },
              { label: 'Net Profit', value: fmt(summary.netProfit ?? 0) },
              { label: 'Sale Count', value: String(summary.saleCount) },
            ].map((item) => (
              <Grid item xs={6} sm={3} key={item.label}>
                <Typography variant="body2" color="text.secondary">{item.label}</Typography>
                <Typography variant="h6" fontWeight="bold">{item.value}</Typography>
              </Grid>
            ))}
          </Grid>
        </Card>
      ) : null}

      {/* Top Products (selected period) */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <TrendingUpIcon fontSize="small" color="primary" />
          <Typography variant="subtitle2" fontWeight="bold">Top Products (selected period)</Typography>
        </Box>
        <Button
          size="small"
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          onClick={handleExportTopProducts}
          disabled={!topProducts?.length}
        >
          Export CSV
        </Button>
      </Stack>
      {topLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress size={24} /></Box>
      ) : (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} md={7}>
            <Card>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Product</TableCell>
                    <TableCell>SKU</TableCell>
                    <TableCell align="right">Qty Sold</TableCell>
                    <TableCell align="right">Revenue (Rs.)</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(topProducts ?? []).map((p) => (
                    <TableRow key={p.productId}>
                      <TableCell>{p.productName}</TableCell>
                      <TableCell>{p.sku}</TableCell>
                      <TableCell align="right">{Number(p.quantitySold).toFixed(0)}</TableCell>
                      <TableCell align="right">{Number(p.revenue).toFixed(2)}</TableCell>
                    </TableRow>
                  ))}
                  {(topProducts ?? []).length === 0 && (
                    <TableRow><TableCell colSpan={4}>No sales in this period.</TableCell></TableRow>
                  )}
                </TableBody>
              </Table>
            </Card>
          </Grid>
          {(topProducts ?? []).length > 0 && (
            <Grid item xs={12} md={5}>
              <Card sx={{ p: 2, height: '100%' }}>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Revenue by Product</Typography>
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={(topProducts ?? []).map((p) => ({ name: p.productName, revenue: p.revenue }))} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis type="number" />
                    <YAxis type="category" dataKey="name" width={110} tick={{ fontSize: 11 }} />
                    <Tooltip formatter={(value) => Number(value).toFixed(2)} />
                    <Bar dataKey="revenue" fill="#2e7d32" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </Card>
            </Grid>
          )}
        </Grid>
      )}

      {/* Profit by Category */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <CategoryIcon fontSize="small" color="primary" />
          <Typography variant="subtitle2" fontWeight="bold">Profit by Product Category</Typography>
        </Box>
        <Button
          size="small"
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          onClick={handleExportCategoryProfit}
          disabled={!categoryProfit?.categories?.length}
        >
          Export CSV
        </Button>
      </Stack>
      {catProfitLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress size={24} /></Box>
      ) : categoryProfit ? (
        <Card sx={{ mb: 3 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Category</TableCell>
                <TableCell align="right">Qty Sold</TableCell>
                <TableCell align="right">Revenue (Rs.)</TableCell>
                <TableCell align="right">COGS (Rs.)</TableCell>
                <TableCell align="right">Profit (Rs.)</TableCell>
                <TableCell align="right">Margin %</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(categoryProfit.categories ?? []).map((row) => {
                const margin = Number(row.profitMarginPct ?? 0);
                const marginColor = margin >= 30 ? 'success' : margin >= 15 ? 'primary' : 'warning';
                return (
                  <TableRow key={row.categoryId || row.categoryName}>
                    <TableCell sx={{ fontWeight: 'bold' }}>{row.categoryName}</TableCell>
                    <TableCell align="right">{Number(row.totalQty).toFixed(0)}</TableCell>
                    <TableCell align="right">{Number(row.totalRevenue).toFixed(2)}</TableCell>
                    <TableCell align="right">{Number(row.totalCogs).toFixed(2)}</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 'bold', color: Number(row.totalProfit) >= 0 ? 'success.main' : 'error.main' }}>
                      {Number(row.totalProfit).toFixed(2)}
                    </TableCell>
                    <TableCell align="right">
                      <Chip label={margin.toFixed(1) + '%'} color={marginColor} size="small" variant="outlined" />
                    </TableCell>
                  </TableRow>
                );
              })}
              {(!categoryProfit.categories || categoryProfit.categories.length === 0) && (
                <TableRow><TableCell colSpan={6}>No sales recorded for any category in this period.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
        </Card>
      ) : null}

      {/* Expense Breakdown by Category */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="subtitle2" fontWeight="bold">Expense Breakdown by Category</Typography>
        <Button
          size="small"
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          onClick={handleExportExpenses}
          disabled={!expenseSummary}
        >
          Export CSV
        </Button>
      </Stack>
      {expenseLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress size={24} /></Box>
      ) : expenseSummary ? (
        <Card sx={{ mb: 3 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Category</TableCell>
                <TableCell align="right">Amount (Rs.)</TableCell>
                <TableCell align="right">Share %</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {Object.entries(expenseSummary.byCategory ?? {}).map(([cat, amt]) => (
                <TableRow key={cat}>
                  <TableCell>{cat}</TableCell>
                  <TableCell align="right">{Number(amt).toFixed(2)}</TableCell>
                  <TableCell align="right">
                    {expenseSummary.totalExpenses > 0
                      ? ((Number(amt) / Number(expenseSummary.totalExpenses)) * 100).toFixed(1) + '%'
                      : '—'}
                  </TableCell>
                </TableRow>
              ))}
              {Object.keys(expenseSummary.byCategory ?? {}).length === 0 && (
                <TableRow><TableCell colSpan={3}>No expenses in this period.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
          <Box sx={{ p: 1.5, display: 'flex', justifyContent: 'flex-end' }}>
            <Chip label={'Total: ' + fmt(Number(expenseSummary.totalExpenses))} color="primary" size="small" />
          </Box>
        </Card>
      ) : null}

      {/* Inventory Valuation */}
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="subtitle2" fontWeight="bold">Inventory Valuation (Current Stock)</Typography>
        <Button
          size="small"
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          onClick={handleExportInventory}
          disabled={!valuation?.length}
        >
          Export CSV
        </Button>
      </Stack>
      {valuationLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}><CircularProgress size={24} /></Box>
      ) : (
        <Card sx={{ mb: 3 }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Product</TableCell>
                <TableCell>SKU</TableCell>
                <TableCell>Branch</TableCell>
                <TableCell align="right">Qty Available</TableCell>
                <TableCell align="right">Avg Cost</TableCell>
                <TableCell align="right">Total Value</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(valuation ?? []).map((r, i) => (
                <TableRow key={i}>
                  <TableCell>{r.productName}</TableCell>
                  <TableCell>{r.sku}</TableCell>
                  <TableCell>{r.branchName}</TableCell>
                  <TableCell align="right">{Number(r.quantityAvailable).toFixed(0)}</TableCell>
                  <TableCell align="right">{Number(r.averageCost).toFixed(2)}</TableCell>
                  <TableCell align="right">{Number(r.totalValue).toFixed(2)}</TableCell>
                </TableRow>
              ))}
              {(valuation ?? []).length === 0 && (
                <TableRow><TableCell colSpan={6}>No inventory data found.</TableCell></TableRow>
              )}
            </TableBody>
          </Table>
          <Box sx={{ p: 1.5, display: 'flex', justifyContent: 'flex-end' }}>
            <Chip label={'Total Stock Value: ' + fmt(totalInventoryValue)} color="success" size="small" />
          </Box>
        </Card>
      )}
    </Box>
  );
}
