import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Tabs, Tab, Table, TableHead, TableRow, TableCell,
  TableBody, CircularProgress, Alert, Chip, Button,
} from '@mui/material';
import api from '../lib/api';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import type { ProductRecommendation, RecommendationType } from '../types';

const TABS: { key: RecommendationType; label: string }[] = [
  { key: 'FAST_MOVING', label: 'Fast Moving' },
  { key: 'HIGH_PROFIT', label: 'High Profit' },
  { key: 'REORDER', label: 'Reorder Now' },
  { key: 'SLOW_MOVING', label: 'Slow Moving' },
];

const PERIODS = [7, 30, 90];

function Fmt({ value }: { value?: number }) {
  return <>{value === undefined ? '-' : Number(value.toFixed(2)).toLocaleString()}</>;
}

export default function RecommendationsPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const [tab, setTab] = useState<RecommendationType>('FAST_MOVING');
  const [days, setDays] = useState(30);

  const { data, isLoading, error } = useQuery({
    queryKey: ['recommendations', shopId, tab, days],
    queryFn: async () => {
      const res = await api.get<{ data: ProductRecommendation[] }>('/recommendations', {
        params: {
          type: tab,
          shopId,
          ...(branchId ? { branchId } : {}),
          ...(tab === 'FAST_MOVING' || tab === 'HIGH_PROFIT' ? { days } : {}),
        },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Recommendations</Typography>

      <Tabs
        value={tab}
        onChange={(_, v) => setTab(v)}
        textColor="primary"
        indicatorColor="primary"
        sx={{ mb: 2 }}
      >
        {TABS.map((t) => (
          <Tab key={t.key} value={t.key} label={t.label} />
        ))}
      </Tabs>

      {(tab === 'FAST_MOVING' || tab === 'HIGH_PROFIT') && (
        <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
          {PERIODS.map((p) => (
            <Button
              key={p}
              size="small"
              variant={days === p ? 'contained' : 'outlined'}
              onClick={() => setDays(p)}
            >
              {p} days
            </Button>
          ))}
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load recommendations.
        </Alert>
      )}

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              {tab === 'FAST_MOVING' && (
                <TableRow>
                  <TableCell>Product</TableCell>
                  <TableCell>SKU</TableCell>
                  <TableCell align="right">Qty Sold</TableCell>
                  <TableCell align="right">Avg Daily</TableCell>
                  <TableCell align="right">Current Stock</TableCell>
                  <TableCell align="right">Score</TableCell>
                </TableRow>
              )}
              {tab === 'HIGH_PROFIT' && (
                <TableRow>
                  <TableCell>Product</TableCell>
                  <TableCell>SKU</TableCell>
                  <TableCell align="right">Revenue</TableCell>
                  <TableCell align="right">Profit</TableCell>
                  <TableCell align="right">Margin %</TableCell>
                  <TableCell align="right">Qty Sold</TableCell>
                </TableRow>
              )}
              {tab === 'REORDER' && (
                <TableRow>
                  <TableCell>Product</TableCell>
                  <TableCell>SKU</TableCell>
                  <TableCell align="right">Current Stock</TableCell>
                  <TableCell align="right">Reorder Level</TableCell>
                  <TableCell align="right">Avg Daily</TableCell>
                  <TableCell align="right">Suggested Qty</TableCell>
                  <TableCell>Supplier</TableCell>
                  <TableCell align="right">Urgency</TableCell>
                </TableRow>
              )}
              {tab === 'SLOW_MOVING' && (
                <TableRow>
                  <TableCell>Product</TableCell>
                  <TableCell>SKU</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Qty Sold (90d)</TableCell>
                  <TableCell align="right">Current Stock</TableCell>
                  <TableCell>Last Sale</TableCell>
                  <TableCell>Suggested Action</TableCell>
                </TableRow>
              )}
            </TableHead>
            <TableBody>
              {(data ?? []).map((r) => (
                <TableRow key={r.productId}>
                  {tab === 'FAST_MOVING' && (
                    <>
                      <TableCell>{r.productName}</TableCell>
                      <TableCell>{r.sku}</TableCell>
                      <TableCell align="right"><Fmt value={r.quantitySold} /></TableCell>
                      <TableCell align="right"><Fmt value={r.avgDailySales} /></TableCell>
                      <TableCell align="right"><Fmt value={r.currentStock} /></TableCell>
                      <TableCell align="right"><Fmt value={r.score} /></TableCell>
                    </>
                  )}
                  {tab === 'HIGH_PROFIT' && (
                    <>
                      <TableCell>{r.productName}</TableCell>
                      <TableCell>{r.sku}</TableCell>
                      <TableCell align="right"><Fmt value={r.revenue} /></TableCell>
                      <TableCell align="right"><Fmt value={r.profit} /></TableCell>
                      <TableCell align="right"><Fmt value={r.marginPercent} /></TableCell>
                      <TableCell align="right"><Fmt value={r.quantitySold} /></TableCell>
                    </>
                  )}
                  {tab === 'REORDER' && (
                    <>
                      <TableCell>{r.productName}</TableCell>
                      <TableCell>{r.sku}</TableCell>
                      <TableCell align="right"><Fmt value={r.currentStock} /></TableCell>
                      <TableCell align="right"><Fmt value={r.reorderLevel} /></TableCell>
                      <TableCell align="right"><Fmt value={r.avgDailySales} /></TableCell>
                      <TableCell align="right"><Fmt value={r.suggestedOrderQty} /></TableCell>
                      <TableCell>{r.supplierName ?? '-'}</TableCell>
                      <TableCell align="right"><Fmt value={r.urgency} /></TableCell>
                    </>
                  )}
                  {tab === 'SLOW_MOVING' && (
                    <>
                      <TableCell>{r.productName}</TableCell>
                      <TableCell>{r.sku}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          color={r.status === 'DEAD_STOCK' ? 'error' : 'warning'}
                          label={r.status ?? '-'}
                        />
                      </TableCell>
                      <TableCell align="right"><Fmt value={r.quantitySold} /></TableCell>
                      <TableCell align="right"><Fmt value={r.currentStock} /></TableCell>
                      <TableCell>{r.lastSaleDate ?? '-'}</TableCell>
                      <TableCell>{r.suggestedAction ?? '-'}</TableCell>
                    </>
                  )}
                </TableRow>
              ))}
              {(data ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={8}>No recommendations for this period. Sell some products and try again.</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
    </Box>
  );
}