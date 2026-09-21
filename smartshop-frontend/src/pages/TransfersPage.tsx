import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  Pagination, CircularProgress, Alert, MenuItem, Chip, IconButton,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import type { Branch, PageResponse, Product } from '../types';

interface TransferRow {
  id: string;
  transferNumber: string;
  fromBranchName: string;
  toBranchName: string;
  status: string;
}

interface Line {
  productId: string;
  quantity: string;
}

export default function TransfersPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [fromBranchId, setFromBranchId] = useState(branchId ?? '');
  const [toBranchId, setToBranchId] = useState('');
  const [lines, setLines] = useState<Line[]>([{ productId: '', quantity: '1' }]);

  // Fetch the shop's real branches (GET /branches returns a plain array). Using
  // the API rather than the user's role grants means shop-admins — whose grant is
  // shop-level with no branchId — still see every branch they can transfer between.
  const { data: branchData } = useQuery({
    queryKey: ['branches', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: Branch[] }>('/branches', { params: { shopId } });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });
  const branches = (branchData ?? []).map((b) => ({ id: b.id, name: b.name }));

  const { data, isLoading } = useQuery({
    queryKey: ['transfers', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<TransferRow> }>('/stock-transfers', {
        params: { shopId, page, size: 10 },
      });
      return res.data.data;
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
      api.post('/stock-transfers', {
        shopId,
        fromBranchId,
        toBranchId,
        note: null,
        items: lines
          .filter((l) => l.productId && Number(l.quantity) > 0)
          .map((l) => ({ productId: l.productId, quantity: Number(l.quantity) })),
      }),
    onSuccess: () => {
      setOpen(false);
      setLines([{ productId: '', quantity: '1' }]);
      queryClient.invalidateQueries({ queryKey: ['transfers'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const act = useMutation({
    mutationFn: async ({ id, action }: { id: string; action: 'approve' | 'reject' }) =>
      api.post(`/stock-transfers/${id}/${action}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['transfers'] });
      queryClient.invalidateQueries({ queryKey: ['inventory'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Stock Transfers</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Transfer
        </Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}
      {branches.length < 2 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Stock transfers move inventory between two branches, so you need at least two.
          Add branches under <strong>Administration → Branches</strong>.
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
                <TableCell>Transfer #</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell>Status</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((t) => (
                <TableRow key={t.id}>
                  <TableCell>{t.transferNumber}</TableCell>
                  <TableCell>{t.fromBranchName}</TableCell>
                  <TableCell>{t.toBranchName}</TableCell>
                  <TableCell>
                    <Chip size="small" color={t.status === 'APPROVED' ? 'success' : t.status === 'REJECTED' ? 'error' : 'warning'} label={t.status} />
                  </TableCell>
                  <TableCell>
                    {t.status === 'PENDING' && (
                      <>
                        <IconButton size="small" color="success" onClick={() => act.mutate({ id: t.id, action: 'approve' })}>
                          <CheckIcon />
                        </IconButton>
                        <IconButton size="small" color="error" onClick={() => act.mutate({ id: t.id, action: 'reject' })}>
                          <CloseIcon />
                        </IconButton>
                      </>
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>No transfers yet</TableCell>
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
        <DialogTitle>New Stock Transfer</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <TextField
              select
              label="From Branch"
              fullWidth
              value={fromBranchId}
              onChange={(e) => setFromBranchId(e.target.value)}
            >
              {branches.map((b) => (
                <MenuItem key={b.id} value={b.id}>
                  {b.name}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="To Branch"
              fullWidth
              value={toBranchId}
              onChange={(e) => setToBranchId(e.target.value)}
            >
              {branches.map((b) => (
                <MenuItem key={b.id} value={b.id}>
                  {b.name}
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
              <IconButton onClick={() => setLines(lines.filter((_, i) => i !== idx))} disabled={lines.length === 1}>
                <DeleteIcon />
              </IconButton>
            </Box>
          ))}
          <Button onClick={() => setLines([...lines, { productId: '', quantity: '1' }])}>Add item</Button>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!fromBranchId || !toBranchId || fromBranchId === toBranchId || create.isPending}
            onClick={() => create.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}