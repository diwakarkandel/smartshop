import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Chip, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api, { extractErrorMessage } from '../lib/api';
import { defaultShopId } from '../stores/shopStore';
import type { PageResponse, Supplier } from '../types';

const FIELDS = ['name', 'companyName', 'phone', 'email', 'address', 'panNumber'] as const;

export default function SuppliersPage() {
  const shopId = defaultShopId();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState<Record<string, string>>({ name: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['suppliers', shopId, page],
    queryFn: async () => {
      const res = await api.get<{ data: PageResponse<Supplier> }>('/suppliers', {
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
      return api.post('/suppliers', payload);
    },
    onSuccess: () => {
      setOpen(false);
      setForm({ name: '' });
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
    },
    onError: (err) => setError(extractErrorMessage(err)),
  });

  const deactivate = useMutation({
    mutationFn: (id: string) => api.delete(`/suppliers/${id}`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['suppliers'] }),
    onError: (err) => setError(extractErrorMessage(err)),
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Suppliers</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
          New Supplier
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
                <TableCell>Company</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Status</TableCell>
                <TableCell></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((s) => (
                <TableRow key={s.id}>
                  <TableCell>{s.name}</TableCell>
                  <TableCell>{s.companyName ?? '-'}</TableCell>
                  <TableCell>{s.phone ?? '-'}</TableCell>
                  <TableCell>{s.email ?? '-'}</TableCell>
                  <TableCell>
                    <Chip size="small" label={s.status} color={s.status === 'ACTIVE' ? 'success' : 'default'} />
                  </TableCell>
                  <TableCell>
                    <IconButton size="small" onClick={() => deactivate.mutate(s.id)}>
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
              {(data?.content ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>No suppliers found</TableCell>
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
        <DialogTitle>New Supplier</DialogTitle>
        <DialogContent>
          {FIELDS.map((f) => (
            <TextField
              key={f}
              label={f.replace(/([A-Z])/g, ' $1')}
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