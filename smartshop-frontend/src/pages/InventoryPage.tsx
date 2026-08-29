import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Chip, Pagination, CircularProgress, Switch, FormControlLabel,
} from '@mui/material';
import api from '../lib/api';
import { defaultBranchId } from '../stores/shopStore';
import type { InventoryItem, PageResponse } from '../types';

export default function InventoryPage() {
  const branchId = defaultBranchId();
  const [page, setPage] = useState(0);
  const [lowOnly, setLowOnly] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['inventory', branchId, page, lowOnly],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<InventoryItem> }>('/inventory', {
        params: { branchId, page, size: 10, lowStockOnly: lowOnly || undefined },
      });
      return res.data.data;
    },
    enabled: Boolean(branchId),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Inventory</Typography>
        <FormControlLabel
          control={<Switch checked={lowOnly} onChange={(e) => setLowOnly(e.target.checked)} />}
          label="Low stock only"
        />
      </Box>
      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Product</TableCell>
                <TableCell>SKU</TableCell>
                <TableCell align="right">Available</TableCell>
                <TableCell align="right">Reserved</TableCell>
                <TableCell align="right">Avg Cost</TableCell>
                <TableCell align="right">Reorder Level</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((i) => (
                <TableRow key={i.id}>
                  <TableCell>{i.productName}</TableCell>
                  <TableCell>{i.sku}</TableCell>
                  <TableCell align="right">{i.quantityAvailable}</TableCell>
                  <TableCell align="right">{i.quantityReserved}</TableCell>
                  <TableCell align="right">{i.averageCost.toFixed(2)}</TableCell>
                  <TableCell align="right">{i.reorderLevel}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={i.stockStatus === 'IN_STOCK' ? 'success' : i.stockStatus === 'LOW_STOCK' ? 'warning' : 'error'}
                      label={i.stockStatus.replace('_', ' ')}
                    />
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={7}>No inventory records for this branch</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>
    </Box>
  );
}