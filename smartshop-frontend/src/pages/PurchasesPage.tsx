import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert, MenuItem, Chip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../lib/api';
import { defaultShopId, defaultBranchId } from '../stores/shopStore';
import type { PageResponse, Product, Purchase, Supplier } from '../types';

interface Line {
  productId: string;
  quantity: string;
  unitCost: string;
}

export default function PurchasesPage() {
  const shopId = defaultShopId();
  const branchId = defaultBranchId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [supplierId, setSupplierId] = useState('');
  const [paymentStatus, setPaymentStatus] = useState('UNPAID');
  const [lines, setLines] = useState<Line[]>([{ productId: '', quantity: '1', unitCost: '0' }]);

  const { data, isLoading } = useQuery({
    queryKey: ['purchases', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Purchase> }>('/purchases', {
        params: { shopId, page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: suppliers } = useQuery({
    queryKey: ['suppliers-simple', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Supplier> }>('/suppliers', {
        params: { shopId, page: 0, size: 100 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const { data: products } = useQuery({
    queryKey: ['products-simple', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Product> }>('/products', {
        params: { shopId, page: 0, size: 100 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const create = useMutation({
    mutationFn: async () =>
      api.post('/purchases', {
        shopId,
        branchId,
        supplierId,
        paymentStatus,
        discountAmount: 0,
        items: lines
          .filter((l) => l.productId && Number(l.quantity) > 0)
          .map((l) => ({
            productId: l.productId,
            quantity: Number(l.quantity),
            unitCost: Number(l.unitCost),
          })),
      }),
    onSuccess: () => {
      setOpen(false);
      setLines([{ productId: '', quantity: '1', unitCost: '0' }]);
      setSupplierId('');
      queryClient.invalidateQueries({ queryKey: ['purchases'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Purchases</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Purchase
        </Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
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
              <TableRow>
                <TableCell>Number</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Supplier</TableCell>
                <TableCell>Branch</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.purchaseNumber}</TableCell>
                  <TableCell>{p.purchaseDate}</TableCell>
                  <TableCell>{p.supplierName}</TableCell>
                  <TableCell>{p.branchName}</TableCell>
                  <TableCell align="right">{p.totalAmount.toFixed(2)}</TableCell>
                  <TableCell>
                    <Chip size="small" color={p.paymentStatus === 'PAID' ? 'success' : 'warning'} label={p.paymentStatus} />
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>No purchases found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>New Purchase</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <TextField
              select
              label="Supplier"
              fullWidth
              value={supplierId}
              onChange={(e) => setSupplierId(e.target.value)}
            >
              {(suppliers ?? []).map((s) => (
                <MenuItem key={s.id} value={s.id}>
                  {s.name}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Payment Status"
              value={paymentStatus}
              onChange={(e) => setPaymentStatus(e.target.value)}
            >
              {['UNPAID', 'PARTIAL', 'PAID'].map((s) => (
                <MenuItem key={s} value={s}>
                  {s}
                </MenuItem>
              ))}
            </TextField>
          </Box>
          {lines.map((line, idx) => (
            <Box key={idx} sx={{ display: 'flex', gap: 1, mb: 1 }}>
              <TextField
                select
                label="Product"
                fullWidth
                size="small"
                value={line.productId}
                onChange={(e) => setLines(lines.map((l, i) => (i === idx ? { ...l, productId: e.target.value } : l)))}
              >
                {(products ?? []).map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.name} ({p.sku})
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="Qty"
                type="number"
                size="small"
                value={line.quantity}
                onChange={(e) => setLines(lines.map((l, i) => (i === idx ? { ...l, quantity: e.target.value } : l)))}
              />
              <TextField
                label="Unit Cost"
                type="number"
                size="small"
                value={line.unitCost}
                onChange={(e) => setLines(lines.map((l, i) => (i === idx ? { ...l, unitCost: e.target.value } : l)))}
              />
              <IconButton
                onClick={() => setLines(lines.filter((_, i) => i !== idx))}
                disabled={lines.length === 1}
              >
                <DeleteIcon />
              </IconButton>
            </Box>
          ))}
          <Button onClick={() => setLines([...lines, { productId: '', quantity: '1', unitCost: '0' }])}>
            Add item
          </Button>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!supplierId || !branchId || create.isPending}
            onClick={() => create.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}