import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Card, Typography, Table, TableHead, TableRow, TableCell, TableBody,
  Button, Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  IconButton, Pagination, CircularProgress, Alert, Stack,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import api from '../lib/api';
import { useCrudDialog } from '../lib/useCrudDialog';
import { useDefaultShopId } from '../stores/shopStore';
import type { Customer, PageResponse } from '../types';

const FIELDS = ['name', 'phone', 'email', 'address'] as const;
type CustomerForm = Record<string, string>;

export default function CustomersPage() {
  const shopId = useDefaultShopId();
  const [page, setPage] = useState(0);

  const { open, editing, form, setForm, error, setError, openNew, openEdit, close, save, remove } =
    useCrudDialog<Customer, CustomerForm>({
      path: '/customers',
      invalidateKey: 'customers',
      emptyForm: { name: '' },
      toForm: (c) => ({ name: c.name, phone: c.phone ?? '', email: c.email ?? '', address: c.address ?? '' }),
      toPayload: (f) => {
        const payload: Record<string, unknown> = { shopId };
        FIELDS.forEach((k) => { payload[k] = f[k] || null; });
        return payload;
      },
    });

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

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h5">Customers</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openNew}>
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
                <TableCell align="right"></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(data?.content ?? []).map((c) => (
                <TableRow key={c.id}>
                  <TableCell>{c.name}</TableCell>
                  <TableCell>{c.phone ?? '-'}</TableCell>
                  <TableCell>{c.email ?? '-'}</TableCell>
                  <TableCell align="right">{c.loyaltyPoints}</TableCell>
                  <TableCell align="right">
                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                      <Button size="small" onClick={() => openEdit(c)}>
                        Edit
                      </Button>
                      <IconButton size="small" onClick={() => {
                        if (!window.confirm(`Delete customer "${c.name}"?`)) return;
                        remove.mutate(c.id);
                      }}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
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

      <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? `Edit Customer - ${editing.name}` : 'New Customer'}</DialogTitle>
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
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" disabled={!form.name || save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
