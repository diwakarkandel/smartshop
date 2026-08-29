import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../lib/api';
import { defaultShopId } from '../stores/shopStore';
import type { Customer, PageResponse } from '../types';

const FIELDS = ['name', 'phone', 'email', 'address'] as const;

export default function CustomersPage() {
  const shopId = defaultShopId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState<Record<string, string>>({ name: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['customers', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Customer> }>('/customers', {
        params: { shopId, page, size: 10 },
      });
      return res.data.data;
    },
    enabled: Boolean(shopId),
  });

  const create = useMutation({
    mutationFn: async () => {
      const payload: Record<string, unknown> = { shopId };
      FIELDS.forEach((f) => {
        payload[f] = form[f] || null;
      });
      return api.post('/customers', payload);
    },
    onSuccess: () => {
      setOpen(false);
      setForm({ name: '' });
      queryClient.invalidateQueries({ queryKey: ['customers'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const remove = useMutation({
    mutationFn: (id: string) => api.delete(`/customers/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['customers'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Customers</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Customer
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
                <TableCell>Name</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Email</TableCell>
                <TableCell align="right">Loyalty</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((c) => (
                <TableRow key={c.id}>
                  <TableCell>{c.name}</TableCell>
                  <TableCell>{c.phone ?? '-'}</TableCell>
                  <TableCell>{c.email ?? '-'}</TableCell>
                  <TableCell align="right">{c.loyaltyPoints}</TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => remove.mutate(c.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>No customers found</TableCell>
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
        <DialogTitle>New Customer</DialogTitle>
        <DialogContent>
          {FIELDS.map((f) => (
            <TextField
              key={f}
              label={f.replace(/^./, (c) => c.toUpperCase())}
              fullWidth
              margin="dense"
              value={form[f] ?? ''}
              onChange={(e) => setForm({ ...form, [f]: e.target.value })}
            />
          ))}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={!form.name || create.isPending} onClick={() => create.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}