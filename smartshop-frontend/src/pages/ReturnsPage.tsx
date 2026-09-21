import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  Pagination, CircularProgress, Alert, MenuItem, IconButton,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import type { PageResponse, Sale, SaleItem } from '../types';

interface SaleDetail extends Sale {
  items: (SaleItem & { productId: string })[];
}

interface Line {
  saleItemId: string;
  productId: string;
  quantity: string;
}

export default function ReturnsPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [saleId, setSaleId] = useState('');
  const [reason, setReason] = useState('');
  const [lines, setLines] = useState<Line[]>([]);

  const { data, isLoading } = useQuery({
    queryKey: ['sale-returns', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<{ id: string; returnNumber: string; returnDate: string; invoiceNumber: string; refundAmount: number }> }>(
        '/sale-returns',
        { params: { shopId, page, size: 10 } },
      );
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const { data: sales } = useQuery({
    queryKey: ['sales-simple', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Sale> }>('/sales', {
        params: { shopId, page: 0, size: 50 },
      });
      return res.data.data.content;
    },
    enabled: Boolean(shopId),
  });

  const loadSale = useQuery({
    queryKey: ['sale-detail', saleId],
    queryFn: async () => {
      const res = await api.get<{ data: SaleDetail }>(`/sales/${saleId}`);
      return res.data.data;
    },
    enabled: Boolean(saleId),
  });

  const onSaleChange = (id: string) => {
    setSaleId(id);
    setLines([]);
  };

  const populateLines = () => {
    if (loadSale.data) {
      setLines(
        loadSale.data.items.map((item) => ({
          saleItemId: item.id,
          productId: item.productId,
          quantity: String(item.quantity),
        })),
      );
    }
  };

  const create = useMutation({
    mutationFn: async () =>
      api.post('/sale-returns', {
        saleId,
        branchId,
        reason: reason || null,
        items: lines
          .filter((l) => Number(l.quantity) > 0)
          .map((l) => ({
            saleItemId: l.saleItemId,
            productId: l.productId,
            quantity: Number(l.quantity),
          })),
      }),
    onSuccess: () => {
      setOpen(false);
      setLines([]);
      setSaleId('');
      setReason('');
      queryClient.invalidateQueries({ queryKey: ['sale-returns'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Sale Returns</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Return
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
                <TableCell>Return #</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Invoice</TableCell>
                <TableCell align="right">Refund</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((r) => (
                <TableRow key={r.id}>
                  <TableCell>{r.returnNumber}</TableCell>
                  <TableCell>{r.returnDate}</TableCell>
                  <TableCell>{r.invoiceNumber}</TableCell>
                  <TableCell align="right">{r.refundAmount.toFixed(2)}</TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={4}>No returns yet</TableCell>
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
        <DialogTitle>New Sale Return</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <TextField
              select
              label="Sale Invoice"
              fullWidth
              value={saleId}
              onChange={(e) => onSaleChange(e.target.value)}
            >
              {(sales ?? []).map((s) => (
                <MenuItem key={s.id} value={s.id}>
                  {s.invoiceNumber} — {s.totalAmount.toFixed(2)}
                </MenuItem>
              ))}
            </TextField>
            <TextField label="Reason" fullWidth value={reason} onChange={(e) => setReason(e.target.value)} />
          </Box>
          {saleId && (
            <>
              <Button onClick={populateLines} disabled={!loadSale.data}>
                Load sale items
              </Button>
              {lines.map((line, idx) => (
                <Box key={idx} sx={{ display: 'flex', gap: 1, mb: 1, mt: 1 }}>
                  <TextField
                    label="Product"
                    size="small"
                    fullWidth
                    disabled
                    value={loadSale.data?.items.find((i) => i.id === line.saleItemId)?.productName ?? ''}
                  />
                  <TextField
                    label="Qty"
                    type="number"
                    size="small"
                    value={line.quantity}
                    onChange={(e) =>
                      setLines(lines.map((l, i) => (i === idx ? { ...l, quantity: e.target.value } : l)))
                    }
                  />
                  <IconButton onClick={() => setLines(lines.filter((_, i) => i !== idx))}>
                    <DeleteIcon />
                  </IconButton>
                </Box>
              ))}
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!saleId || !branchId || create.isPending}
            onClick={() => create.mutate()}
          >
            Submit Return
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}