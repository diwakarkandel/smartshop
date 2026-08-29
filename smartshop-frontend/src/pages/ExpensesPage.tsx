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
import type { Expense, PageResponse } from '../types';

export default function ExpensesPage() {
  const shopId = defaultShopId();
  const branchId = defaultBranchId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({ title: '', category: '', amount: '0', paymentMethod: 'CASH', note: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['expenses', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Expense> }>('/expenses', {
        params: { shopId, page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const create = useMutation({
    mutationFn: async () =>
      api.post('/expenses', {
        shopId,
        branchId: branchId ?? null,
        title: form.title,
        category: form.category || null,
        amount: Number(form.amount),
        paymentMethod: form.paymentMethod,
        note: form.note || null,
      }),
    onSuccess: () => {
      setOpen(false);
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/expenses/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['expenses'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Expenses</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Expense
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
                <TableCell>Title</TableCell>
                <TableCell>Category</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Method</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((e) => (
                <TableRow key={e.id}>
                  <TableCell>{e.title}</TableCell>
                  <TableCell>{e.category ?? '-'}</TableCell>
                  <TableCell>{e.expenseDate}</TableCell>
                  <TableCell align="right">{e.amount.toFixed(2)}</TableCell>
                  <TableCell>
                    <Chip size="small" label={e.paymentMethod ?? 'CASH'} />
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => remove.mutate(e.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>No expenses recorded</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </Card>
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
        <Pagination count={data?.totalPages ?? 1} page={page + 1} onChange={(_, p) => setPage(p - 1)} />
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New Expense</DialogTitle>
        <DialogContent>
          <TextField label="Title" fullWidth margin="dense" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <TextField label="Category" fullWidth margin="dense" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
          <TextField label="Amount" type="number" fullWidth margin="dense" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
          <TextField
            select
            label="Payment Method"
            fullWidth
            margin="dense"
            value={form.paymentMethod}
            onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}
          >
            {['CASH', 'CARD', 'ESEWA', 'KHALTI', 'BANK_TRANSFER'].map((m) => (
              <MenuItem key={m} value={m}>
                {m}
              </MenuItem>
            ))}
          </TextField>
          <TextField label="Note" fullWidth margin="dense" multiline value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!form.title || create.isPending} onClick={() => create.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}