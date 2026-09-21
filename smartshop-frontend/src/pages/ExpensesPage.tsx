import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert, MenuItem, Chip, Stack, Divider,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import CategoryIcon from '@mui/icons-material/Category';
import api, { extractErrorMessage } from '../lib/api';
import { useDefaultShopId, useDefaultBranchId } from '../stores/shopStore';
import type { Expense, ExpenseCategory, PageResponse } from '../types';

const EMPTY_FORM = { title: '', category: '', amount: '0', paymentMethod: 'CASH', note: '', expenseDate: '' };

export default function ExpensesPage() {
  const shopId = useDefaultShopId();
  const branchId = useDefaultBranchId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState<Expense | null>(null);
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [catManagerOpen, setCatManagerOpen] = useState(false);

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

  // Scope to the active shop: without shopId the backend returns EVERY shop's
  // categories, and the key must include shopId so switching shops refetches.
  const { data: categories } = useQuery({
    queryKey: ['expense-categories', shopId],
    queryFn: async () => {
      const res = await api.get<{ data: ExpenseCategory[] }>('/expense-categories', {
        params: { shopId },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const openNew = () => {
    setEditing(null);
    setForm({ ...EMPTY_FORM });
    setOpen(true);
  };

  const openEdit = (e: Expense) => {
    setEditing(e);
    setForm({
      title: e.title,
      category: e.category ?? '',
      amount: String(e.amount ?? 0),
      paymentMethod: e.paymentMethod ?? 'CASH',
      note: e.note ?? '',
      expenseDate: e.expenseDate ?? '',
    });
    setOpen(true);
  };

  const closeDialog = () => {
    setOpen(false);
    setEditing(null);
    setForm({ ...EMPTY_FORM });
  };

  const save = useMutation({
    mutationFn: async () => {
      const payload = {
        shopId,
        branchId: branchId ?? null,
        title: form.title,
        category: form.category || null,
        amount: Number(form.amount),
        paymentMethod: form.paymentMethod,
        note: form.note || null,
        expenseDate: form.expenseDate || null,
      };
      return editing ? api.put(`/expenses/${editing.id}`, payload) : api.post('/expenses', payload);
    },
    onSuccess: () => {
      closeDialog();
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
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" startIcon={<CategoryIcon />} onClick={() => setCatManagerOpen(true)}>
            Manage Categories
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={openNew}>
            New Expense
          </Button>
        </Stack>
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
                <TableCell align="right"></TableCell>
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
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" onClick={() => openEdit(e)}>
                        Edit
                      </Button>
                      <IconButton size="small" onClick={() => {
                        if (!window.confirm(`Delete expense "${e.title}"?`)) return;
                        remove.mutate(e.id);
                      }}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
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

      <Dialog open={open} onClose={closeDialog} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? `Edit Expense - ${editing.title}` : 'New Expense'}</DialogTitle>
        <DialogContent>
          <TextField label="Title" fullWidth margin="dense" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <TextField
            select
            label="Category"
            fullWidth
            margin="dense"
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })}
          >
            <MenuItem value="">— None —</MenuItem>
            {(categories ?? []).map((c) => (
              <MenuItem key={c.id} value={c.name}>
                {c.name}
              </MenuItem>
            ))}
          </TextField>
          <TextField label="Amount" type="number" fullWidth margin="dense" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} />
          <TextField
            label="Expense Date"
            type="date"
            fullWidth
            margin="dense"
            InputLabelProps={{ shrink: true }}
            value={form.expenseDate}
            onChange={(e) => setForm({ ...form, expenseDate: e.target.value })}
          />
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
          <Button onClick={closeDialog}>Cancel</Button>
          <Button variant="contained" disabled={!form.title || save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <ExpenseCategoryManager
        open={catManagerOpen}
        onClose={() => setCatManagerOpen(false)}
        shopId={shopId}
        categories={categories ?? []}
      />
    </Box>
  );
}

function ExpenseCategoryManager({
  open,
  onClose,
  shopId,
  categories,
}: {
  open: boolean;
  onClose: () => void;
  shopId: string | null;
  categories: ExpenseCategory[];
}) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState({ name: '', description: '' });

  const reset = () => {
    setEditingId(null);
    setForm({ name: '', description: '' });
  };

  const save = useMutation({
    mutationFn: async () => {
      const body = { name: form.name, description: form.description || null };
      return editingId
        ? api.put(`/expense-categories/${editingId}`, body)
        : api.post('/expense-categories', body, { params: { shopId } });
    },
    onSuccess: () => {
      reset();
      queryClient.invalidateQueries({ queryKey: ['expense-categories'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/expense-categories/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['expense-categories'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Dialog open={open} onClose={() => { reset(); onClose(); }} fullWidth maxWidth="sm">
      <DialogTitle>Manage Expense Categories</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
            {error}
          </Alert>
        )}
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell align="right"></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {categories.map((c) => (
              <TableRow key={c.id}>
                <TableCell>{c.name}</TableCell>
                <TableCell>{c.description ?? '-'}</TableCell>
                <TableCell align="right">
                  <Stack direction="row" spacing={1} justifyContent="flex-end">
                    <Button size="small" onClick={() => { setEditingId(c.id); setForm({ name: c.name, description: c.description ?? '' }); }}>
                      Edit
                    </Button>
                    <IconButton size="small" onClick={() => {
                      if (!window.confirm(`Delete category "${c.name}"?`)) return;
                      remove.mutate(c.id);
                    }}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Stack>
                </TableCell>
              </TableRow>
            ))}
            {categories.length === 0 && (
              <TableRow>
                <TableCell colSpan={3}>No categories yet</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>

        <Divider sx={{ my: 2 }} />
        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          {editingId ? 'Edit category' : 'Add category'}
        </Typography>
        {!editingId && !shopId && (
          <Alert severity="info" sx={{ mb: 1 }}>
            Select a shop first — new categories are added to the active shop.
          </Alert>
        )}
        <TextField label="Name" fullWidth margin="dense" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <TextField label="Description" fullWidth margin="dense" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
          <Button
            variant="contained"
            disabled={!form.name || save.isPending || (!editingId && !shopId)}
            onClick={() => save.mutate()}
          >
            {editingId ? 'Update' : 'Add'}
          </Button>
          {editingId && (
            <Button onClick={reset}>Cancel edit</Button>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={() => { reset(); onClose(); }}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
